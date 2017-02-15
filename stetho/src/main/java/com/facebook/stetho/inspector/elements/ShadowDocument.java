/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import android.app.Activity;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.Util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class ShadowDocument implements DocumentView {
  private final NodeID mRootNodeID;
  private final IdentityHashMap<NodeID, NodeInfo> mNodeIDToInfoMap = new IdentityHashMap<>();
  private boolean mIsUpdating;

  public ShadowDocument(NodeID rootID) {
    mRootNodeID = Util.throwIfNull(rootID);
  }

  @Override
  public NodeID getRootID() {
    return mRootNodeID;
  }

  @Override
  public NodeInfo getNodeInfo(NodeID id) {
    return mNodeIDToInfoMap.get(id);
  }

  public UpdateBuilder beginUpdate() {
    if (mIsUpdating) {
      throw new IllegalStateException();
    }

    mIsUpdating = true;

    return new UpdateBuilder();
  }

  public final class UpdateBuilder {
    /**
     * We use a {@link LinkedHashMap} to preserve ordering between
     * {@link UpdateBuilder#setNodeChildren(NodeID, List)} and
     * {@link Update#getChangedNodeIDs(Accumulator)}. This isn't needed for correctness but it
     * significantly improves performance.<p/>
     *
     * Transmitting DOM updates to Chrome works best if we can do it in top-down order because it
     * allows us to skip processing (and, more importantly, transmission) of an nodeID that was
     * already transmitted in a previous DOM.childNodeInserted event (i.o.w. we can skip
     * transmission of E2 if it was already bundled up in E1's event, where E2 is any nodeID in
     * E1's sub-tree). DOM.childNodeInserted transmits the nodeID being inserted by-value, so it takes
     * time and space proportional to the size of that nodeID's sub-tree. This means the difference
     * between O(n^2) and O(n) time for transmitting updates to Chrome.<p/>
     *
     * We currently only have one implementation of {@link DocumentProvider},
     * {@link com.facebook.stetho.inspector.elements.android.AndroidDocumentProvider}, and it
     * already supplies nodeID changes in top-down order. Because of this, we can just use
     * {@link LinkedHashMap} instead of adding some kind of post-process sorting of the elements to
     * put them in that order. If we reach a point where we can't or shouldn't rely on elements
     * being forwarded to us in top-down order, then we should change this field to an
     * {@link IdentityHashMap} and sort them before relaying them via
     * {@link Update#getChangedNodeIDs(Accumulator)}.<p/>
     *
     * When a large sub-tree is added (e.g. starting a new {@link Activity}), the use of
     * {@link LinkedHashMap} instead of {@link IdentityHashMap} can mean the difference between an
     * update taking 500ms versus taking more than 30 seconds.<p/>
     *
     * Technically we actually want something like a LinkedIdentityHashMap because we do want
     * to key off of object identity instead of allowing for the possibility of value identity.
     * Given the difference in performance, however, the risk of potential protocol abuse seems
     * reasonable.<p/>
     */
    private final Map<NodeID, NodeInfo> mNodeIDToInfoChangesMap = new LinkedHashMap<>();

    /**
     * This contains every nodeID in {@link #mNodeIDToInfoChangesMap} whose
     * {@link NodeInfo#parentID} is null. {@link ShadowDocument} provides access to a tree, which
     * means it has a single root (only one nodeID with a null parent). During an update, however,
     * the DOM can be conceptually thought of as being a forest. The true root is identified by
     * {@link #mRootNodeID}, and all other roots identify disconnected trees full of elements that
     * must be garbage collected.
     */
    private final HashSet<NodeID> mRootNodeIDChanges = new HashSet<>();

    /**
     * This is used during {@link #setNodeChildren}. We allocate 1 on-demand and reuse it.
     */
    private HashSet<NodeID> mCachedNotNewChildrenSet;

    public void setNodeChildren(NodeID nodeID, List<NodeID> childrenIDs) {
      // If we receive redundant information, then nothing needs to be done.
      NodeInfo changesNodeInfo = mNodeIDToInfoChangesMap.get(nodeID);
      if (changesNodeInfo != null &&
          childrenIDs.equals(changesNodeInfo.childrenIDs)) {
        return;
      }

      NodeInfo oldNodeInfo = mNodeIDToInfoMap.get(nodeID);
      if (changesNodeInfo == null &&
          oldNodeInfo != null &&
          childrenIDs.equals(oldNodeInfo.childrenIDs)) {
        return;
      }

      NodeInfo newNodeInfo;
      if (changesNodeInfo != null &&
          oldNodeInfo != null &&
          oldNodeInfo.parentID.equals(changesNodeInfo.parentID) &&
          childrenIDs.equals(oldNodeInfo.childrenIDs)) {
        // setNodeChildren() was already called for nodeID with changes during this
        // transaction, but now we're being told that the childrenIDs should match the old view.
        // So we should actually remove the change entry.
        newNodeInfo = mNodeIDToInfoMap.get(nodeID);
        mNodeIDToInfoChangesMap.remove(nodeID);
      } else {
        NodeID parentID = (changesNodeInfo != null)
            ? changesNodeInfo.parentID
            : (oldNodeInfo != null)
            ? oldNodeInfo.parentID
            : null;

        newNodeInfo = new NodeInfo(nodeID, parentID, childrenIDs);

        mNodeIDToInfoChangesMap.put(nodeID, newNodeInfo);
      }

      // At this point, newNodeInfo is either equal to oldNodeInfo because we've reverted
      // back to the same data that's in the old view of the tree, or it's a brand new object with
      // brand new changes (it's different than both of oldNodeInfo and changesNodeInfo).

      // Next, set the parentID to null for child ids that have been removed from
      // nodeID's childrenIDs. We must be careful not to set a parentID to null if that child has
      // already been moved to be the child of a different nodeID. e.g.,
      //     setNodeChildren(E, { A, B, C})
      //     ...
      //     setNodeChildren(F, { A })
      //     setNodeChildren(E, { B, C })    (don't mark A's parent as null in this case)

      // notNewChildrenSet = (oldChildren + changesChildren) - newChildren
      HashSet<NodeID> notNewChildrenSet = acquireNotNewChildrenHashSet();

      if (oldNodeInfo != null &&
          !oldNodeInfo.childrenIDs.equals(newNodeInfo.childrenIDs)) {
        for (int i = 0, N = oldNodeInfo.childrenIDs.size(); i < N; ++i) {
          final NodeID childID = oldNodeInfo.childrenIDs.get(i);
          notNewChildrenSet.add(childID);
        }
      }

      if (changesNodeInfo != null &&
          !changesNodeInfo.childrenIDs.equals(newNodeInfo.childrenIDs)) {
        for (int i = 0, N = changesNodeInfo.childrenIDs.size(); i < N; ++i) {
          final NodeID childID = changesNodeInfo.childrenIDs.get(i);
          notNewChildrenSet.add(childID);
        }
      }

      for (int i = 0, N = newNodeInfo.childrenIDs.size(); i < N; ++i) {
        final NodeID childID = newNodeInfo.childrenIDs.get(i);
        setNodeParent(childID, nodeID);
        notNewChildrenSet.remove(childID);
      }

      for (NodeID childID : notNewChildrenSet) {
        final NodeInfo childChangesNodeInfo = mNodeIDToInfoChangesMap.get(childID);
        if (childChangesNodeInfo != null &&
            !childChangesNodeInfo.parentID.equals(nodeID)) {
          // do nothing. this childID was moved to be the child of another nodeID.
          continue;
        }

        final NodeInfo oldChangesNodeInfo = mNodeIDToInfoMap.get(childID);
        if (oldChangesNodeInfo != null &&
            oldChangesNodeInfo.parentID.equals(nodeID)) {
          setNodeParent(childID, null);
        }
      }

      releaseNotNewChildrenHashSet(notNewChildrenSet);
    }

    private void setNodeParent(NodeID nodeID, NodeID parentID) {
      NodeInfo changesNodeInfo = mNodeIDToInfoChangesMap.get(nodeID);
      if (changesNodeInfo != null &&
          parentID.equals(changesNodeInfo.parentID)) {
        return;
      }

      NodeInfo oldNodeInfo = mNodeIDToInfoMap.get(nodeID);
      if (changesNodeInfo == null &&
          oldNodeInfo != null &&
          parentID.equals(oldNodeInfo.parentID)) {
        return;
      }

      if (changesNodeInfo != null &&
          oldNodeInfo != null &&
          parentID.equals(oldNodeInfo.parentID) &&
          oldNodeInfo.childrenIDs.equals(changesNodeInfo.childrenIDs)) {
        mNodeIDToInfoChangesMap.remove(nodeID);

        if (parentID == null) {
          mRootNodeIDChanges.remove(nodeID);
        }

        return;
      }

      List<NodeID> children = (changesNodeInfo != null)
          ? changesNodeInfo.childrenIDs
          : (oldNodeInfo != null)
          ? oldNodeInfo.childrenIDs
          : Collections.<NodeID>emptyList();

      NodeInfo newNodeInfo = new NodeInfo(nodeID, parentID, children);
      mNodeIDToInfoChangesMap.put(nodeID, newNodeInfo);

      if (parentID == null) {
        mRootNodeIDChanges.add(nodeID);
      } else {
        mRootNodeIDChanges.remove(nodeID);
      }
    }

    public Update build() {
      return new Update(mNodeIDToInfoChangesMap, mRootNodeIDChanges);
    }

    private HashSet<NodeID> acquireNotNewChildrenHashSet() {
      HashSet<NodeID> notNewChildrenHashSet = mCachedNotNewChildrenSet;
      if (notNewChildrenHashSet == null) {
        notNewChildrenHashSet = new HashSet<>();
      }
      mCachedNotNewChildrenSet = null;
      return notNewChildrenHashSet;
    }

    private void releaseNotNewChildrenHashSet(HashSet<NodeID> notNewChildrenHashSet) {
      notNewChildrenHashSet.clear();
      if (mCachedNotNewChildrenSet == null) {
        mCachedNotNewChildrenSet = notNewChildrenHashSet;
      }
    }
  }

  public final class Update implements DocumentView {
    private final Map<NodeID, NodeInfo> mNodeIDToInfoChangesMap;
    private final Set<NodeID> mRootNodeIDChangesSet;

    public Update(
        Map<NodeID, NodeInfo> nodeIDToInfoChangesMap,
        Set<NodeID> rootNodeIDChangesSet) {
      mNodeIDToInfoChangesMap = nodeIDToInfoChangesMap;
      mRootNodeIDChangesSet = rootNodeIDChangesSet;
    }

    public boolean isEmpty() {
      return mNodeIDToInfoChangesMap.isEmpty();
    }

    public boolean isNodeIDChanged(NodeID id) {
      return mNodeIDToInfoChangesMap.containsKey(id);
    }

    public NodeID getRootID() {
      return ShadowDocument.this.getRootID();
    }

    public NodeInfo getNodeInfo(NodeID id) {
      // Return NodeInfo for the new (albeit uncommitted and pre-garbage collected) view of the
      // Document. If nodeID is garbage then you'll still get its info (feature, not a bug :)).
      NodeInfo nodeInfo = mNodeIDToInfoChangesMap.get(id);
      if (nodeInfo != null) {
        return nodeInfo;
      }
      return mNodeIDToInfoMap.get(id);
    }

    public void getChangedNodeIDs(Accumulator<NodeID> accumulator) {
      for (NodeID id : mNodeIDToInfoChangesMap.keySet()) {
        accumulator.store(id);
      }
    }

    public void getGarbageNodeIDs(Accumulator<NodeID> accumulator) {
      // This queue stores pairs of elements, [nodeID, expectedParent]
      // When we dequeue, we look at nodeID's parentID in the new view to see if it matches
      // expectedParent. If it does, then it's garbage. For enqueueing roots, whose parents are
      // null, since we can't enqueue null we instead enqueue the nodeID twice.
      Queue<NodeID> queue = new ArrayDeque<>();

      // Initialize the queue with all disconnected tree roots (parentID == null) which
      // aren't the DOM root.
      for (NodeID id : mRootNodeIDChangesSet) {
        NodeInfo newNodeInfo = getNodeInfo(id);
        if (!id.equals(mRootNodeID) && newNodeInfo.parentID == null) {
          queue.add(id);
          queue.add(id);
        }
      }

      // BFS traversal from those elements in the old view of the tree and test each nodeID
      // to see if it's still within a disconnected sub-tree. We can tell if it's garbage if its
      // parent nodeID in the new view of the tree hasn't changed.
      while (!queue.isEmpty()) {
        final NodeID id = queue.remove();
        final NodeID expectedParent0 = queue.remove();
        final NodeID expectedParent = id.equals(expectedParent0) ? null : expectedParent0;
        final NodeInfo newNodeInfo = getNodeInfo(id);

        if (newNodeInfo.parentID.equals(expectedParent)) {
          accumulator.store(id);

          NodeInfo oldNodeInfo = ShadowDocument.this.getNodeInfo(id);
          if (oldNodeInfo != null) {
            for (int i = 0, N = oldNodeInfo.childrenIDs.size(); i < N; ++i) {
              queue.add(oldNodeInfo.childrenIDs.get(i));
              queue.add(id);
            }
          }
        }
      }
    }

    public void abandon() {
      if (!mIsUpdating) {
        throw new IllegalStateException();
      }

      mIsUpdating = false;
    }

    public void commit() {
      if (!mIsUpdating) {
        throw new IllegalStateException();
      }

      // Apply the changes to the tree
      mNodeIDToInfoMap.putAll(mNodeIDToInfoChangesMap);

      // Remove garbage elements: those that have a null parent (other than mRootNodeID), and
      // their entire sub-trees, but excluding reparented elements.
      for (NodeID id : mRootNodeIDChangesSet) {
        removeGarbageSubTree(mNodeIDToInfoMap, id);
      }

      mIsUpdating = false;

      // Not usually enabled because it's expensive. Very useful for debugging.
      //validateTree(mNodeIDToInfoMap);
    }

    private void removeGarbageSubTree(
        Map<NodeID, NodeInfo> nodeIDToInfoMap,
        NodeID nodeID) {
      final NodeInfo nodeInfo = nodeIDToInfoMap.get(nodeID);

      // If this nodeID has a parent (it's not a root), and that parent is still in the tree after
      // changes have been applied and after our caller (removeGarbageSubTree) removed another
      // nodeID that claims this nodeID as its child, then that means this nodeID should not be
      // removed. It has been reparented, and recursion stops here.
      if (nodeInfo.parentID != null &&
          nodeIDToInfoMap.containsKey(nodeInfo.parentID)) {
        return;
      }

      nodeIDToInfoMap.remove(nodeID);

      for (int i = 0, N = nodeInfo.childrenIDs.size(); i < N; ++i) {
        removeGarbageSubTree(nodeIDToInfoMap, nodeInfo.childrenIDs.get(i));
      }
    }

    // This method is intended for use during debugging. Put a breakpoint on each throw statement in
    // order to catch structural problems in the tree. This method should only be called at the very
    // end of commit().
    private void validateTree(Map<NodeID, NodeInfo> nodeIDToInfoMap) {
      // We need a tree, not a forest.
      HashSet<NodeID> rootIDs = new HashSet<>();

      for (Map.Entry<NodeID, NodeInfo> entry : nodeIDToInfoMap.entrySet()) {
        final NodeID nodeI = entry.getKey();
        final NodeInfo nodeInfo = entry.getValue();

        if (!nodeI.equals(nodeInfo.nodeID)) {
          // should not be possible
          throw new IllegalStateException("nodeID != nodeInfo.nodeID");
        }

        // Verify childrenIDs
        for (int i = 0, N = nodeInfo.childrenIDs.size(); i < N; ++i) {
          final NodeID childID = nodeInfo.childrenIDs.get(i);
          final NodeInfo childNodeInfo = nodeIDToInfoMap.get(childID);

          if (childNodeInfo == null) {
            throw new IllegalStateException(String.format(
                "nodeInfo.get(nodeInfo.childrenIDs.get(%s)) == null",
                i));
          }

          if (!childNodeInfo.parentID.equals(nodeI)) {
            throw new IllegalStateException("childNodeInfo.parentID != nodeID");
          }
        }

        // Verify parent
        if (nodeInfo.parentID == null) {
          rootIDs.add(nodeI);
        } else {
          final NodeInfo parentNodeInfo = nodeIDToInfoMap.get(nodeInfo.parentID);
          if (parentNodeInfo == null) {
            throw new IllegalStateException(
                "nodeIDToInfoMap.get(nodeInfo.parentNodeInfo) == NULL");
          }

          if (!nodeInfo.parentID.equals(parentNodeInfo.nodeID)) {
            // should not be possible
            throw new IllegalStateException(
                "nodeInfo.parentNodeInfo != parentNodeInfo.parent");
          }

          if (!parentNodeInfo.childrenIDs.contains(nodeI)) {
            throw new IllegalStateException(
                "parentNodeInfo.childrenIDs.contains(nodeID) == FALSE");
          }
        }
      }

      if (rootIDs.size() != 1) {
        throw new IllegalStateException(
            "nodeIDToInfoMap is a forest, not a tree. rootrootIDs.size() != 1");
      }
    }
  }
}

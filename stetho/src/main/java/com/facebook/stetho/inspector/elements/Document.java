/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import android.os.SystemClock;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ArrayListAccumulator;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.inspector.helper.ThreadBoundProxy;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

public final class Document extends ThreadBoundProxy {
  private final DocumentProviderFactory mFactory;
  private final Map<NodeID, Object> mNodeIDToElement;
  private final Queue<Object> mCachedUpdateQueue;

  private DocumentProvider mDocumentProvider;
  private ShadowDocument mShadowDocument;
  private UpdateListenerCollection mUpdateListeners;
  private ChildEventingList mCachedChildEventingList;
  private ArrayListAccumulator<Object> mCachedChildrenAccumulator;
  private AttributeListAccumulator mCachedAttributeAccumulator;

  @GuardedBy("this")
  private int mReferenceCounter;

  public Document(DocumentProviderFactory factory) {
    super(factory);

    mFactory = factory;
    mNodeIDToElement = new HashMap<>();
    mReferenceCounter = 0;
    mUpdateListeners = new UpdateListenerCollection();
    mCachedUpdateQueue = new ArrayDeque<>();
  }

  public synchronized void addRef() {
    if (mReferenceCounter++ == 0) {
      init();
    }
  }

  public synchronized void release() {
    if (mReferenceCounter > 0) {
      if (--mReferenceCounter == 0) {
        cleanUp();
      }
    }
  }

  private void init() {
    mDocumentProvider = mFactory.create();

    mDocumentProvider.postAndWait(new Runnable() {
      @Override
      public void run() {
        final Object rootElement = mDocumentProvider.getRootElement();
        final NodeDescriptor descriptor = getNodeDescriptor(rootElement);
        final NodeID rootID = descriptor.getNodeID(rootElement);
        mNodeIDToElement.put(rootID, mDocumentProvider.getRootElement());
        mShadowDocument = new ShadowDocument(rootID);
        createShadowDocumentUpdate().commit();
        mDocumentProvider.setListener(new ProviderListener());
      }
    });

  }

  private void cleanUp() {
    mDocumentProvider.postAndWait(new Runnable() {
      @Override
      public void run() {
        mDocumentProvider.setListener(null);
        mShadowDocument = null;

        for (Object element : mNodeIDToElement.values()) {
          getNodeDescriptor(element).unhook(element);
        }
        mNodeIDToElement.clear();

        mDocumentProvider.dispose();
        mDocumentProvider = null;
      }
    });

    mUpdateListeners.clear();
  }

  public void addUpdateListener(UpdateListener updateListener) {
    mUpdateListeners.add(updateListener);
  }

  public void removeUpdateListener(UpdateListener updateListener) {
    mUpdateListeners.remove(updateListener);
  }

  public @Nullable NodeDescriptor getNodeDescriptor(Object element) {
    verifyThreadAccess();
    return mDocumentProvider.getNodeDescriptor(element);
  }

  public void highlightElement(Object element, int color) {
    verifyThreadAccess();
    mDocumentProvider.highlightElement(element, color);
  }

  public void hideHighlight() {
    verifyThreadAccess();
    mDocumentProvider.hideHighlight();
  }

  public void setInspectModeEnabled(boolean enabled) {
    verifyThreadAccess();
    mDocumentProvider.setInspectModeEnabled(enabled);
  }

  public @Nullable NodeID getNodeIdForElement(Object element) {
    // We don't actually call verifyThreadAccess() for performance.
    //verifyThreadAccess();

    final NodeID id = getNodeDescriptor(element).getNodeID(element);
    return id == null || mNodeIDToElement.containsKey(id) ? id : null;
  }

  public @Nullable Object getElementForNodeId(Integer id) {
    return id == null ? null : getElementForNodeId(new NodeID(id));
  }

  public @Nullable Object getElementForNodeId(NodeID id) {
    // We don't actually call verifyThreadAccess() for performance.
    //verifyThreadAccess();
    return mNodeIDToElement.get(id);
  }

  public void setAttributesAsText(Object element, String text) {
    verifyThreadAccess();
    mDocumentProvider.setAttributesAsText(element, text);
  }

  public void getElementStyles(Object element, StyleAccumulator styleAccumulator) {
    NodeDescriptor nodeDescriptor = getNodeDescriptor(element);

    nodeDescriptor.getStyles(element, styleAccumulator);
  }

  public void getElementAccessibilityStyles(Object element, StyleAccumulator styleAccumulator) {
    NodeDescriptor nodeDescriptor = getNodeDescriptor(element);

    nodeDescriptor.getAccessibilityStyles(element, styleAccumulator);
  }

  public DocumentView getDocumentView() {
    verifyThreadAccess();
    return mShadowDocument;
  }

  public Object getRootElement() {
    verifyThreadAccess();

    Object rootElement = mDocumentProvider.getRootElement();
    if (rootElement == null) {
      // null for rootElement is not allowed. We could support it, but our current
      // implementation won't ever run into this, so let's punt on it for now.
      throw new IllegalStateException();
    }

    if (!mShadowDocument.getRootID().equals(getNodeIdForElement(rootElement))) {
      // We don't support changing the root nodeID. This is handled differently by the
      // protocol than updates to an existing DOM, and we don't have any case in our
      // current implementation that causes this to happen, so let's punt on it for now.
      throw new IllegalStateException();
    }

    return rootElement;
  }

  public void findMatchingElements(String query, Accumulator<NodeID> matchedIds) {
    verifyThreadAccess();

    final Pattern queryPattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
    final Object rootElement = mDocumentProvider.getRootElement();

    findMatches(getNodeIdForElement(rootElement), queryPattern, matchedIds);
  }

  private void findMatches(NodeID nodeID, Pattern queryPattern, Accumulator<NodeID> matchedIds) {
    final NodeInfo info = mShadowDocument.getNodeInfo(nodeID);

    for (int i = 0, size = info.childrenIDs.size(); i < size; i++) {
      final NodeID childID = info.childrenIDs.get(i);

      if (doesElementMatch(childID, queryPattern)) {
        matchedIds.store(nodeID);
      }

      findMatches(childID, queryPattern, matchedIds);
    }
  }

  private boolean doesElementMatch(NodeID nodeID, Pattern queryPattern) {
    final Object element = getElementForNodeId(nodeID);
    AttributeListAccumulator accumulator = acquireCachedAttributeAccumulator();
    NodeDescriptor descriptor = mDocumentProvider.getNodeDescriptor(element);

    descriptor.getAttributes(element, accumulator);

    for (int i = 0, N = accumulator.size(); i < N; i++) {
      if (queryPattern.matcher(accumulator.get(i)).find()) {
        releaseCachedAttributeAccumulator(accumulator);
        return true;
      }
    }

    releaseCachedAttributeAccumulator(accumulator);
    return queryPattern.matcher(descriptor.getNodeName(element)).find();
  }

  private ChildEventingList acquireChildEventingList(
      NodeID parentElement,
      DocumentView documentView) {
    ChildEventingList childEventingList = mCachedChildEventingList;

    if (childEventingList == null) {
      childEventingList = new ChildEventingList();
    }

    mCachedChildEventingList = null;

    childEventingList.acquire(parentElement, documentView);
    return childEventingList;
  }

  private void releaseChildEventingList(ChildEventingList childEventingList) {
    childEventingList.release();
    if (mCachedChildEventingList == null) {
      mCachedChildEventingList = childEventingList;
    }
  }

  private AttributeListAccumulator acquireCachedAttributeAccumulator() {
    AttributeListAccumulator accumulator = mCachedAttributeAccumulator;

    if (accumulator == null) {
      accumulator = new AttributeListAccumulator();
    }

    mCachedChildrenAccumulator = null;

    return accumulator;
  }

  private void releaseCachedAttributeAccumulator(AttributeListAccumulator accumulator) {
    accumulator.clear();

    if (mCachedAttributeAccumulator == null) {
      mCachedAttributeAccumulator = accumulator;
    }
  }

  private ArrayListAccumulator<Object> acquireChildrenAccumulator() {
    ArrayListAccumulator<Object> accumulator = mCachedChildrenAccumulator;
    if (accumulator == null) {
      accumulator = new ArrayListAccumulator<>();
    }
    mCachedChildrenAccumulator = null;
    return accumulator;
  }

  private void releaseChildrenAccumulator(ArrayListAccumulator<Object> accumulator) {
    accumulator.clear();
    if (mCachedChildrenAccumulator == null) {
      mCachedChildrenAccumulator = accumulator;
    }
  }

  private ShadowDocument.Update createShadowDocumentUpdate() {
    verifyThreadAccess();

    if (!mShadowDocument.getRootID().equals(getNodeIdForElement(mDocumentProvider.getRootElement()))) {
      throw new IllegalStateException();
    }

    ArrayListAccumulator<Object> childrenAccumulator = acquireChildrenAccumulator();

    ShadowDocument.UpdateBuilder updateBuilder = mShadowDocument.beginUpdate();
    mCachedUpdateQueue.add(mDocumentProvider.getRootElement());

    while (!mCachedUpdateQueue.isEmpty()) {
      final Object element = mCachedUpdateQueue.remove();
      NodeDescriptor descriptor = mDocumentProvider.getNodeDescriptor(element);

      final NodeID nodeId = descriptor.getNodeID(element);
      if (!mNodeIDToElement.containsKey(nodeId)) {
        mNodeIDToElement.put(nodeId, element);
        descriptor.hook(element);
      }

      descriptor.getChildren(element, childrenAccumulator);

      for (int i = 0, size = childrenAccumulator.size(); i < size; ++i) {
        Object child = childrenAccumulator.get(i);
        if (child != null) {
          mCachedUpdateQueue.add(child);
        } else {
          // This could be indicative of a bug in Stetho code, but could also be caused by a
          // custom nodeID of some kind, e.g. ViewGroup. Let's not allow it to kill the hosting
          // app.
          LogUtil.e(
              "%s.getChildren() emitted a null child at position %s for nodeID %s",
              descriptor.getClass().getName(),
              Integer.toString(i),
              element);

          childrenAccumulator.remove(i);
          --i;
          --size;
        }
      }

      final List<NodeID> childrenIds = new ArrayList<>(childrenAccumulator.size());
      for (int i = 0, size = childrenAccumulator.size(); i < size; ++i) {
        final Object childElement = childrenAccumulator.get(i);
        final NodeDescriptor childDescriptor = getNodeDescriptor(childElement);
        final NodeID childID = childDescriptor.getNodeID(childElement);
        if (!mNodeIDToElement.containsKey(childID)) {
          mNodeIDToElement.put(childID, childElement);
          childDescriptor.hook(childElement);
        }
        childrenIds.add(childID);
      }

      updateBuilder.setNodeChildren(nodeId, childrenIds);
      childrenAccumulator.clear();
    }

    releaseChildrenAccumulator(childrenAccumulator);

    return updateBuilder.build();
  }

  private void updateTree() {
    long startTimeMs = SystemClock.elapsedRealtime();

    ShadowDocument.Update docUpdate = createShadowDocumentUpdate();
    boolean isEmpty = docUpdate.isEmpty();
    if (isEmpty) {
      docUpdate.abandon();
    } else {
      applyDocumentUpdate(docUpdate);
    }

    long deltaMs = SystemClock.elapsedRealtime() - startTimeMs;
    LogUtil.d(
        "Document.updateTree() completed in %s ms%s",
        Long.toString(deltaMs),
        isEmpty ? " (no changes)" : "");
  }

  private void applyDocumentUpdate(final ShadowDocument.Update docUpdate) {
    // TODO: it'd be nice if we could delegate our calls into mPeerManager.sendNotificationToPeers()
    //       to a background thread so as to offload the UI from JSON serialization stuff

    // Applying the ShadowDocument.Update is done in five stages:

    // Stage 1: any elements that have been disconnected from the tree, and any elements in those
    // sub-trees which have not been reconnected to the tree, should be garbage collected. For now
    // we gather a list of garbage nodeID IDs which we use in stages 2 to test a changed nodeID
    // to see if it's also garbage. Then during stage 3 we use this list to unhook all of the
    // garbage elements.

    // This is used to collect the garbage nodeID IDs in stage 1. It is sorted before stage 2 so
    // that it can use a binary search as a quick "contains()" method.
    // Note that this could be accomplished in a simpler way by employing a HashSet<Object> and
    // storing the nodeID Objects. However, HashSet wraps HashMap and we would have a lot more
    // allocations (Map.Entry, iterator during stage 3) and thus GC pressure.
    // Using SparseArray wouldn't be good because it ensures sorted ordering as you go, but we don't
    // need that during stage 1. Using ArrayList with int boxing is fine because the Integers are
    // already boxed inside of mNodeIDToElement and we make sure to reuse that allocation.
    final ArrayList<NodeID> garbageNodeIds = new ArrayList<>();

    docUpdate.getGarbageNodeIDs(new Accumulator<NodeID>() {
      @Override
      public void store(NodeID nodeID) {
        NodeInfo newNodeInfo = docUpdate.getNodeInfo(nodeID);

        // Only raise onChildNodeRemoved for the root of a disconnected tree. The remainder of the
        // sub-tree is included automatically, so we don't need to send events for those.
        if (newNodeInfo.parentID == null) {
          NodeInfo oldNodeInfo = mShadowDocument.getNodeInfo(nodeID);
          mUpdateListeners.onChildNodeRemoved(oldNodeInfo.parentID, nodeID);
        }

        garbageNodeIds.add(nodeID);
      }
    });

    Collections.sort(garbageNodeIds);

    // Stage 2: remove all elements that have been reparented. Otherwise we get into trouble if we
    // transmit an event to insert under the new parent before we've transmitted an event to remove
    // it from the old parent. The removal event is ignored because the parent doesn't match the
    // listener's expectations, so we get ghost elements that are stuck and can't be exorcised.
    docUpdate.getChangedNodeIDs(new Accumulator<NodeID>() {
      @Override
      public void store(NodeID nodeID) {
        // Skip garbage elements
        if (Collections.binarySearch(garbageNodeIds, nodeID) >= 0) {
          return;
        }

        // Skip new elements
        final NodeInfo oldNodeInfo = mShadowDocument.getNodeInfo(nodeID);
        if (oldNodeInfo == null) {
          return;
        }

        final NodeInfo newNodeInfo = docUpdate.getNodeInfo(nodeID);
        if (newNodeInfo.parentID.equals(oldNodeInfo.parentID)) {
          mUpdateListeners.onChildNodeRemoved(oldNodeInfo.parentID, nodeID);
        }
      }
    });

    // Stage 3: unhook garbage elements
    for (int i = 0, N = garbageNodeIds.size(); i < N; ++i) {
      final NodeID garbageID = garbageNodeIds.get(i);
      final Object garbage = mNodeIDToElement.get(garbageID);

      if (mNodeIDToElement.containsKey(garbageID)) {
        mNodeIDToElement.remove(garbageID);
        getNodeDescriptor(garbage).unhook(garbage);
      }
    }

    // Stage 4: transmit all other changes to our listener. This includes inserting reparented
    // elements that we removed in the 2nd stage.
    docUpdate.getChangedNodeIDs(new Accumulator<NodeID>() {
      private final HashSet<NodeID> listenerInsertedNodeIDs = new HashSet<>();

      private Accumulator<NodeID> insertedNodeIDs = new Accumulator<NodeID>() {
        @Override
        public void store(NodeID nodeID) {
          if (docUpdate.isNodeIDChanged(nodeID)) {
            // We only need to track changed elements because unchanged elements will never be
            // encountered by the code below, in store(), which uses this Set to skip elements that
            // don't need to be processed.
            listenerInsertedNodeIDs.add(nodeID);
          }
        }
      };

      @Override
      public void store(NodeID nodeID) {
        if (mNodeIDToElement.containsKey(nodeID)) {
          // The nodeID was garbage and has already been removed. At this stage that's okay and we
          // just skip it and continue forward with the algorithm.
          return;
        }

        if (listenerInsertedNodeIDs.contains(nodeID)) {
          // This nodeID was already transmitted in its entirety by an onChildNodeInserted event.
          // Trying to send any further updates about it is both unnecessary and incorrect (we'd
          // end up with duplicated elements and really bad performance).
          return;
        }

        final NodeInfo oldNodeInfo = mShadowDocument.getNodeInfo(nodeID);
        final NodeInfo newNodeInfo = docUpdate.getNodeInfo(nodeID);

        final List<NodeID> oldChildren = (oldNodeInfo != null)
            ? oldNodeInfo.childrenIDs
            : Collections.<NodeID>emptyList();

        final List<NodeID> newChildren = newNodeInfo.childrenIDs;

        // This list is representative of our listener's view of the Document (ultimately, this
        // means Chrome DevTools). We need to sync it up with newChildren.
        ChildEventingList listenerChildren = acquireChildEventingList(nodeID, docUpdate);
        for (int i = 0, N = oldChildren.size(); i < N; ++i) {
          final NodeID childID = oldChildren.get(i);
          final NodeInfo newChildNodeInfo = docUpdate.getNodeInfo(childID);
          if (newChildNodeInfo != null &&
                  newChildNodeInfo.parentID.equals(nodeID)) {
            // This nodeID was reparented, so we already told our listener to remove it.
          } else {
            listenerChildren.add(childID);
          }
        }

        updateListenerChildren(listenerChildren, newChildren, insertedNodeIDs);
        releaseChildEventingList(listenerChildren);
      }
    });

    // Stage 5: Finally, commit the update to the ShadowDocument.
    docUpdate.commit();
  }

  private static void updateListenerChildren(
      ChildEventingList listenerChildrenIDs,
      List<NodeID> newChildIDs,
      Accumulator<NodeID> insertedNodeIDs) {
    int index = 0;
    while (index <= listenerChildrenIDs.size()) {
      // Insert new items that were added to the end of the list
      if (index == listenerChildrenIDs.size()) {
        if (index == newChildIDs.size()) {
          break;
        }

        final NodeID newNodeID = newChildIDs.get(index);
        listenerChildrenIDs.addWithEvent(index, newNodeID, insertedNodeIDs);
        ++index;
        continue;
      }

      // Remove old items that were removed from the end of the list
      if (index == newChildIDs.size()) {
        listenerChildrenIDs.removeWithEvent(index);
        continue;
      }

      final NodeID listenerNodeID = listenerChildrenIDs.get(index);
      final NodeID newNodeID = newChildIDs.get(index);

      // This slot has exactly what we need to have here.
      if (listenerNodeID.equals(newNodeID)) {
        ++index;
        continue;
      }

      int newNodeIDListenerIndex = listenerChildrenIDs.indexOf(newNodeID);
      if (newNodeIDListenerIndex == -1) {
        listenerChildrenIDs.addWithEvent(index, newNodeID, insertedNodeIDs);
        ++index;
        continue;
      }

      // TODO: use longest common substring to decide whether to
      //       1) remove(newElementListenerIndex)-then-add(index), or
      //       2) remove(index) and let a subsequent loop iteration do add() (that is, when index
      //          catches up the current value of newElementListenerIndex)
      //       Neither one of these is the best strategy -- it depends on context.

      listenerChildrenIDs.removeWithEvent(newNodeIDListenerIndex);
      listenerChildrenIDs.addWithEvent(index, newNodeID, insertedNodeIDs);

      ++index;
    }
  }

  /**
   * A private implementation of {@link List} that transmits our changes to our listener (and,
   * ultimately, to the DevTools client).
   */
  private final class ChildEventingList extends ArrayList<NodeID> {
    private NodeID mParentID = null;
    private DocumentView mDocumentView;

    public void acquire(NodeID parentID, DocumentView documentView) {
      mParentID = parentID;
      mDocumentView = documentView;
    }

    public void release() {
      clear();

      mParentID = null;
      mDocumentView = null;
    }

    public void addWithEvent(int index, NodeID nodeID, final Accumulator<NodeID> insertedNodeIDs) {
      final NodeID previousID = (index == 0) ? null : get(index - 1);
      add(index, nodeID);

      mUpdateListeners.onChildNodeInserted(
          mDocumentView,
          getElementForNodeId(nodeID),
          mParentID,
          previousID,
          new Accumulator<Object>() {
            @Override
            public void store(Object element) {
              insertedNodeIDs.store(getNodeIdForElement(element));
            }
          });
    }

    public void removeWithEvent(int index) {
      NodeID nodeId = remove(index);
      mUpdateListeners.onChildNodeRemoved(mParentID, nodeId);
    }
  }

  private class UpdateListenerCollection implements UpdateListener {
    private final List<UpdateListener> mListeners;
    private volatile UpdateListener[] mListenersSnapshot;

    public UpdateListenerCollection() {
      mListeners = new ArrayList<>();
    }

    public synchronized void add(UpdateListener listener) {
      mListeners.add(listener);
      mListenersSnapshot = null;
    }

    public synchronized void remove(UpdateListener listener) {
      mListeners.remove(listener);
      mListenersSnapshot = null;
    }

    public synchronized void clear() {
      mListeners.clear();
      mListenersSnapshot = null;
    }

    private UpdateListener[] getListenersSnapshot() {
      while (true) {
        final UpdateListener[] listenersSnapshot = mListenersSnapshot;
        if (listenersSnapshot != null) {
          return listenersSnapshot;
        }
        synchronized (this) {
          if (mListenersSnapshot == null) {
            mListenersSnapshot = mListeners.toArray(new UpdateListener[mListeners.size()]);
            return mListenersSnapshot;
          }
        }
      }
    }

    @Override
    public void onAttributeModified(Object element, String name, String value) {
      for (UpdateListener listener : getListenersSnapshot()) {
        listener.onAttributeModified(element, name, value);
      }
    }

    @Override
    public void onAttributeRemoved(Object element, String name) {
      for (UpdateListener listener : getListenersSnapshot()) {
        listener.onAttributeRemoved(element, name);
      }
    }

    @Override
    public void onInspectRequested(Object element) {
      for (UpdateListener listener : getListenersSnapshot()) {
        listener.onInspectRequested(element);
      }
    }

    @Override
    public void onChildNodeRemoved(NodeID parentNodeId, NodeID nodeId) {
      for (UpdateListener listener : getListenersSnapshot()) {
        listener.onChildNodeRemoved(parentNodeId, nodeId);
      }
    }

    @Override
    public void onChildNodeInserted(
        DocumentView view,
        Object element,
        NodeID parentNodeId,
        NodeID previousNodeId,
        Accumulator<Object> insertedItems) {
      for (UpdateListener listener : getListenersSnapshot()) {
        listener.onChildNodeInserted(view, element, parentNodeId, previousNodeId, insertedItems);
      }
    }
  }

  public interface UpdateListener {
    void onAttributeModified(Object element, String name, String value);

    void onAttributeRemoved(Object element, String name);

    void onInspectRequested(Object element);

    void onChildNodeRemoved(
        NodeID parentNodeId,
        NodeID nodeId);

    void onChildNodeInserted(
        DocumentView view,
        Object element,
        NodeID parentNodeId,
        NodeID previousNodeId,
        Accumulator<Object> insertedItems);
  }

  private final class ProviderListener implements DocumentProviderListener {
    @Override
    public void onPossiblyChanged() {
      updateTree();
    }

    @Override
    public void onAttributeModified(Object element, String name, String value) {
      verifyThreadAccess();
      mUpdateListeners.onAttributeModified(element, name, value);
    }

    @Override
    public void onAttributeRemoved(Object element, String name) {
      verifyThreadAccess();
      mUpdateListeners.onAttributeRemoved(element, name);
    }

    @Override
    public void onInspectRequested(Object element) {
      verifyThreadAccess();
      mUpdateListeners.onInspectRequested(element);
    }
  }

  public static final class AttributeListAccumulator
    extends ArrayList<String> implements AttributeAccumulator {

    @Override
    public void store(String name, String value) {
      add(name);
      add(value);
    }
  }
}

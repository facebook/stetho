/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.graphics.Color;
import android.os.SystemClock;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ArrayListAccumulator;
import com.facebook.stetho.common.ListUtil;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.UncheckedCallable;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.DOMProvider;
import com.facebook.stetho.inspector.elements.DOMView;
import com.facebook.stetho.inspector.elements.ElementInfo;
import com.facebook.stetho.inspector.elements.ShadowDOM;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.ObjectIdMapper;
import com.facebook.stetho.inspector.helper.PeersRegisteredListener;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcException;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcError;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;

import org.json.JSONObject;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

public class DOM implements ChromeDevtoolsDomain {
  private final ChromePeerManager mPeerManager;
  private final DOMProvider.Factory mDOMProviderFactory;
  private final ObjectMapper mObjectMapper;
  private final DOMObjectIdMapper mObjectIdMapper;
  private final NodeFactory mNodeFactory = new NodeFactory();

  @Nullable
  private volatile DOMProvider mDOMProvider;
  private ShadowDOM mShadowDOM;

  private AttributeListAccumulator mCachedAttributeAccumulator;
  private ArrayListAccumulator<Object> mCachedChildrenAccumulator;
  private ChildEventingList mCachedChildEventingList;
  private ChildNodeRemovedEvent mCachedChildNodeRemovedEvent;
  private ChildNodeInsertedEvent mCachedChildNodeInsertedEvent;

  public DOM(DOMProvider.Factory providerFactory) {
    mDOMProviderFactory = Util.throwIfNull(providerFactory);

    mPeerManager = new ChromePeerManager();
    mPeerManager.setListener(new PeerManagerListener());

    mObjectMapper = new ObjectMapper();
    mObjectIdMapper = new DOMObjectIdMapper();
  }

  @ChromeDevtoolsMethod
  public void enable(JsonRpcPeer peer, JSONObject params) {
    mPeerManager.addPeer(peer);
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
    mPeerManager.removePeer(peer);
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult getDocument(JsonRpcPeer peer, JSONObject params) {
    final GetDocumentResponse result = new GetDocumentResponse();

    result.root = mDOMProvider.postAndWait(new UncheckedCallable<Node>() {
      @Override
      public Node call() {
        Object rootElement = mDOMProvider.getRootElement();
        if (rootElement == null) {
          // null for rootElement is not allowed. We could support it, but our current
          // implementation won't ever run into this, so let's punt on it for now.
          throw new IllegalStateException();
        }

        if (mShadowDOM == null) {
          mShadowDOM = new ShadowDOM(mDOMProvider.getRootElement());
          createShadowDOMUpdate().commit();
          mDOMProvider.setListener(new ProviderListener());
        } else if (rootElement != mShadowDOM.getRootElement()) {
          // We don't support changing the root element. This is handled differently by the
          // protocol than updates to an existing DOM, and we don't have any case in our
          // current implementation that causes this to happen, so let's punt on it for now.
          throw new IllegalStateException();
        }

        return mNodeFactory.createNodeForElement(mShadowDOM, rootElement);
      }
    });

    return result;
  }

  @ChromeDevtoolsMethod
  public void highlightNode(JsonRpcPeer peer, JSONObject params) {
    final HighlightNodeRequest request = mObjectMapper.convertValue(params, HighlightNodeRequest.class);
    if (request.nodeId == null) {
      LogUtil.w("DOM.highlightNode was not given a nodeId; JS objectId is not supported");
      return;
    }

    final RGBAColor contentColor = request.highlightConfig.contentColor;
    if (contentColor == null) {
      LogUtil.w("DOM.highlightNode was not given a color to highlight with");
      return;
    }

    mDOMProvider.postAndWait(new Runnable() {
      @Override
      public void run() {
        Object element = mObjectIdMapper.getObjectForId(request.nodeId);
        if (element != null) {
          mDOMProvider.highlightElement(element, contentColor.getColor());
        }
      }
    });
  }

  @ChromeDevtoolsMethod
  public void hideHighlight(JsonRpcPeer peer, JSONObject params) {
    mDOMProvider.postAndWait(new Runnable() {
      @Override
      public void run() {
        mDOMProvider.hideHighlight();
      }
    });
  }

  @ChromeDevtoolsMethod
  public ResolveNodeResponse resolveNode(JsonRpcPeer peer, JSONObject params)
      throws JsonRpcException {
    ResolveNodeRequest request = mObjectMapper.convertValue(params, ResolveNodeRequest.class);
    Object object = mObjectIdMapper.getObjectForId(request.nodeId);
    if (object == null) {
      throw new JsonRpcException(
          new JsonRpcError(
              JsonRpcError.ErrorCode.INVALID_PARAMS,
              "No known nodeId=" + request.nodeId,
              null /* data */));
    }

    int mappedObjectId = Runtime.mapObject(peer, object);

    Runtime.RemoteObject remoteObject = new Runtime.RemoteObject();
    remoteObject.type = Runtime.ObjectType.OBJECT;
    remoteObject.subtype = Runtime.ObjectSubType.NODE;
    remoteObject.className = object.getClass().getName();
    remoteObject.value = null; // not a primitive
    remoteObject.description = null; // not sure what this does...
    remoteObject.objectId = String.valueOf(mappedObjectId);
    ResolveNodeResponse response = new ResolveNodeResponse();
    response.object = remoteObject;

    return response;
  }

  @ChromeDevtoolsMethod
  public void setAttributesAsText(JsonRpcPeer peer, JSONObject params) {
    final SetAttributesAsTextRequest request = mObjectMapper.convertValue(
        params, SetAttributesAsTextRequest.class);
    final Object object = mObjectIdMapper.getObjectForId(request.nodeId);

    mDOMProvider.postAndWait(new Runnable() {
      @Override
      public void run() {
        mDOMProvider.setAttributesAsText(object, request.text);
      }
    });
  }

  @ChromeDevtoolsMethod
  public void setInspectModeEnabled(JsonRpcPeer peer, JSONObject params) {
    final SetInspectModeEnabledRequest request = mObjectMapper.convertValue(
        params,
        SetInspectModeEnabledRequest.class);

    mDOMProvider.postAndWait(new Runnable() {
      @Override
      public void run() {
        mDOMProvider.setInspectModeEnabled(request.enabled);
      }
    });
  }

  private void updateTree() {
    long startTimeMs = SystemClock.elapsedRealtime();

    ShadowDOM.Update domUpdate = createShadowDOMUpdate();
    boolean isEmpty = domUpdate.isEmpty();
    if (isEmpty) {
      domUpdate.abandon();
    } else {
      applyDOMUpdate(domUpdate);
    }

    long deltaMs = SystemClock.elapsedRealtime() - startTimeMs;
    LogUtil.d(
        "DOM.updateTree() completed in %s ms%s",
        Long.toString(deltaMs),
        isEmpty ? " (no changes)" : "");
  }

  private ShadowDOM.Update createShadowDOMUpdate() {
    mDOMProvider.verifyThreadAccess();

    if (mDOMProvider.getRootElement() != mShadowDOM.getRootElement()) {
      throw new IllegalStateException();
    }

    ArrayListAccumulator<Object> childrenAccumulator = acquireChildrenAccumulator();

    ShadowDOM.UpdateBuilder updateBuilder = mShadowDOM.beginUpdate();
    Queue<Object> queue = new ArrayDeque<>();
    queue.add(mDOMProvider.getRootElement());

    while (!queue.isEmpty()) {
      final Object element = queue.remove();
      NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(element);
      mObjectIdMapper.putObject(element);
      descriptor.getChildren(element, childrenAccumulator);
      updateBuilder.setElementChildren(element, childrenAccumulator);
      for (int i = 0, N = childrenAccumulator.size(); i < N; ++i) {
        queue.add(childrenAccumulator.get(i));
      }
      childrenAccumulator.clear();
    }

    releaseChildrenAccumulator(childrenAccumulator);

    return updateBuilder.build();
  }

  private void applyDOMUpdate(final ShadowDOM.Update domUpdate) {
    // TODO: it'd be nice if we could delegate our calls into mPeerManager.sendNotificationToPeers()
    //       to a background thread so as to offload the UI from JSON serialization stuff

    // First, any elements that have been disconnected from the tree, and any elements in those
    // sub-trees which have not been reconnected to the tree, should be garbage collected.
    // We do this first so that we can tag nodes as garbage by removing them from mObjectIdMapper
    // (which also unhooks them). We rely on this marking later.
    domUpdate.getGarbageElements(new Accumulator<Object>() {
      @Override
      public void store(Object element) {
        if (!mObjectIdMapper.containsObject(element)) {
          throw new IllegalStateException();
        }

        ElementInfo newElementInfo = domUpdate.getElementInfo(element);

        // Only send over DOM.childNodeRemoved for the root of a disconnected tree. The remainder
        // of the sub-tree is included automatically, so we don't need to send events for those.
        if (newElementInfo.parentElement == null) {
          ChildNodeRemovedEvent removedEvent = acquireChildNodeRemovedEvent();
          ElementInfo oldElementInfo = mShadowDOM.getElementInfo(element);
          removedEvent.parentNodeId = mObjectIdMapper.getIdForObject(oldElementInfo.parentElement);
          removedEvent.nodeId = mObjectIdMapper.getIdForObject(element);
          mPeerManager.sendNotificationToPeers("DOM.childNodeRemoved", removedEvent);
          releaseChildNodeRemovedEvent(removedEvent);
        }

        // All garbage elements should be unhooked.
        mObjectIdMapper.removeObject(element);
      }
    });

    // Transmit other DOM changes over to Chrome
    domUpdate.getChangedElements(new Accumulator<Object>() {
      private final HashSet<Object> domInsertedElements = new HashSet<>();

      private final NodeFactory nodeFactory = new NodeFactory() {
        @Override
        public Node createNodeForElement(DOMView domView, Object element) {
          if (domUpdate.isElementChanged(element)) {
            // We only need to track changed elements because unchanged elements will never be
            // encountered by the code below, in store(), which uses this Set to skip elements that
            // don't need to be processed.
            domInsertedElements.add(element);
          }
          return super.createNodeForElement(domView, element);
        }
      };

      @Override
      public void store(Object element) {
        // If this returns false then it means the element was garbage, and has already been removed
        if (!mObjectIdMapper.containsObject(element)) {
          return;
        }

        if (domInsertedElements.contains(element)) {
          // This element was already transmitted in its entirety by a DOM.childNodeInserted event.
          // Trying to send any further updates about it is both unnecessary and incorrect (we'd
          // end up with duplicated elements and really bad performance).
          return;
        }

        final ElementInfo newElementInfo = domUpdate.getElementInfo(element);
        final ElementInfo oldElementInfo = mShadowDOM.getElementInfo(element);

        final List<Object> oldChildren = (oldElementInfo != null)
            ? oldElementInfo.children
            : Collections.emptyList();

        final List<Object> newChildren = newElementInfo.children;

        // This list is representative of Chrome's view of the DOM.
        // We need to sync up Chrome with newChildren.
        ChildEventingList domChildren = acquireChildEventingList(element, domUpdate);
        for (int i = 0, N = oldChildren.size(); i < N; ++i) {
          final Object childElement = oldChildren.get(i);
          if (mObjectIdMapper.containsObject(childElement)) {
            domChildren.add(childElement);
          }
        }
        updateDOMChildren(domChildren, newChildren, nodeFactory);
        releaseChildEventingList(domChildren);
      }
    });

    domUpdate.commit();
  }

  private static void updateDOMChildren(
      ChildEventingList domChildren,
      List<Object> newChildren,
      NodeFactory nodeFactory) {
    int index = 0;
    while (index <= domChildren.size()) {
      // Insert new items that were added to the end of the list
      if (index == domChildren.size()) {
        if (index == newChildren.size()) {
          break;
        }

        final Object newElement = newChildren.get(index);
        domChildren.addWithEvent(index, newElement, nodeFactory);
        ++index;
        continue;
      }

      // Remove old items that were removed from the end of the list
      if (index == newChildren.size()) {
        domChildren.removeWithEvent(index);
        continue;
      }

      final Object domElement = domChildren.get(index);
      final Object newElement = newChildren.get(index);

      // This slot has exactly what we need to have here.
      if (domElement == newElement) {
        ++index;
        continue;
      }

      int newElementDomIndex = domChildren.indexOf(newElement);
      if (newElementDomIndex == -1) {
        domChildren.addWithEvent(index, newElement, nodeFactory);
        ++index;
        continue;
      }

      // TODO: use longest common substring to decide whether to
      //       1) remove(newElementDomIndex)-then-add(index), or
      //       2) remove(index) and let a subsequent loop iteration do add() (that is, when index
      //          catches up the current value of newElementDomIndex)
      //       Neither one of these is the best strategy -- it depends on context.

      domChildren.removeWithEvent(newElementDomIndex);
      domChildren.addWithEvent(index, newElement, nodeFactory);

      ++index;
    }
  }

  private final class PeerManagerListener extends PeersRegisteredListener {
    @Override
    protected synchronized void onFirstPeerRegistered() {
      mDOMProvider = mDOMProviderFactory.create();
      // We dont call mDOMProvider.setListener() until the first DOM.getDocument request.
    }

    @Override
    protected synchronized void onLastPeerUnregistered() {
      mDOMProvider.postAndWait(new Runnable() {
        @Override
        public void run() {
          mDOMProvider.setListener(null);
          mShadowDOM = null;
          mObjectIdMapper.clear();
          mDOMProvider.dispose();
        }
      });

      mDOMProvider = null;
    }
  }

  private AttributeListAccumulator getCachedAttributeListAccumulator() {
    AttributeListAccumulator accumulator = mCachedAttributeAccumulator;
    if (accumulator == null) {
      accumulator = new AttributeListAccumulator();
    }
    mCachedAttributeAccumulator = null;
    return accumulator;
  }

  private void returnCachedAttributeListAccumulator(AttributeListAccumulator accumulator) {
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

  private ChildEventingList acquireChildEventingList(Object parentElement, DOMView domView) {
    ChildEventingList childEventingList = mCachedChildEventingList;
    if (childEventingList == null) {
      childEventingList = new ChildEventingList();
    }
    childEventingList.acquire(parentElement, domView);
    return childEventingList;
  }

  private void releaseChildEventingList(ChildEventingList childEventingList) {
    childEventingList.release();
    if (mCachedChildEventingList == null) {
      mCachedChildEventingList = childEventingList;
    }
  }

  private ChildNodeInsertedEvent acquireChildNodeInsertedEvent() {
    ChildNodeInsertedEvent childNodeInsertedEvent = mCachedChildNodeInsertedEvent;
    if (childNodeInsertedEvent == null) {
      childNodeInsertedEvent = new ChildNodeInsertedEvent();
    }
    mCachedChildNodeInsertedEvent = null;
    return childNodeInsertedEvent;
  }

  private void releaseChildNodeInsertedEvent(ChildNodeInsertedEvent childNodeInsertedEvent) {
    childNodeInsertedEvent.parentNodeId = -1;
    childNodeInsertedEvent.previousNodeId = -1;
    childNodeInsertedEvent.node = null;
    if (mCachedChildNodeInsertedEvent == null) {
      mCachedChildNodeInsertedEvent = childNodeInsertedEvent;
    }
  }

  private ChildNodeRemovedEvent acquireChildNodeRemovedEvent() {
    ChildNodeRemovedEvent childNodeRemovedEvent = mCachedChildNodeRemovedEvent;
    if (childNodeRemovedEvent == null) {
      childNodeRemovedEvent = new ChildNodeRemovedEvent();
    }
    mCachedChildNodeRemovedEvent = null;
    return childNodeRemovedEvent;
  }

  private void releaseChildNodeRemovedEvent(ChildNodeRemovedEvent childNodeRemovedEvent) {
    childNodeRemovedEvent.parentNodeId = -1;
    childNodeRemovedEvent.nodeId = -1;
    if (mCachedChildNodeRemovedEvent == null) {
      mCachedChildNodeRemovedEvent = childNodeRemovedEvent;
    }
  }

  private static final class AttributeListAccumulator
      extends ArrayList<String> implements AttributeAccumulator {

    @Override
    public void store(String name, String value) {
      add(name);
      add(value);
    }
  }

  private class NodeFactory {
    public Node createNodeForElement(DOMView domView, Object element) {
      mDOMProvider.verifyThreadAccess();

      NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(element);

      Node node = new Node();
      node.nodeId = mObjectIdMapper.putObject(element);
      node.nodeType = descriptor.getNodeType(element);
      node.nodeName = descriptor.getNodeName(element);
      node.localName = descriptor.getLocalName(element);
      node.nodeValue = descriptor.getNodeValue(element);

      // Attributes
      AttributeListAccumulator attributeAccumulator = getCachedAttributeListAccumulator();
      descriptor.getAttributes(element, attributeAccumulator);
      node.attributes = ListUtil.copyToImmutableList(attributeAccumulator);
      returnCachedAttributeListAccumulator(attributeAccumulator);

      // Children
      ElementInfo elementInfo = domView.getElementInfo(element);
      List<Node> childrenNodes = (elementInfo.children.size() == 0)
          ? Collections.<Node>emptyList()
          : new ArrayList<Node>(elementInfo.children.size()); // TODO: pool?

      for (int i = 0, N = elementInfo.children.size(); i < N; ++i) {
        final Object childElement = elementInfo.children.get(i);
        Node childNode = createNodeForElement(domView, childElement);
        childrenNodes.add(childNode);
      }

      node.children = childrenNodes;
      node.childNodeCount = childrenNodes.size();

      return node;
    }
  }

  /**
   * A private implementation of {@link List} that transmits DOM changes to Chrome.
   */
  private final class ChildEventingList extends ArrayList<Object> {
    private Object mParentElement = null;
    private int mParentNodeId = -1;
    private DOMView mDOMView;

    public void acquire(Object parentElement, DOMView domView) {
      mParentElement = parentElement;

      mParentNodeId = (mParentElement == null)
          ? -1
          : mObjectIdMapper.getIdForObject(mParentElement);

      mDOMView = domView;
    }

    public void release() {
      clear();

      mParentElement = null;
      mParentNodeId = -1;
      mDOMView = null;
    }

    public void addWithEvent(int index, Object element, NodeFactory nodeFactory) {
      Object previousElement = (index == 0) ? null : get(index - 1);

      int previousNodeId = (previousElement == null)
          ? -1
          : mObjectIdMapper.getIdForObject(previousElement);

      add(index, element);

      ChildNodeInsertedEvent insertedEvent = acquireChildNodeInsertedEvent();
      insertedEvent.parentNodeId = mParentNodeId;
      insertedEvent.previousNodeId = previousNodeId;
      insertedEvent.node = nodeFactory.createNodeForElement(mDOMView, element);
      mPeerManager.sendNotificationToPeers("DOM.childNodeInserted", insertedEvent);
      releaseChildNodeInsertedEvent(insertedEvent);
    }

    public void removeWithEvent(int index) {
      Object element = remove(index);
      ChildNodeRemovedEvent removedEvent = acquireChildNodeRemovedEvent();
      removedEvent.parentNodeId = mParentNodeId;
      removedEvent.nodeId = mObjectIdMapper.getIdForObject(element);
      mPeerManager.sendNotificationToPeers("DOM.childNodeRemoved", removedEvent);
      releaseChildNodeRemovedEvent(removedEvent);
    }
  }

  private final class DOMObjectIdMapper extends ObjectIdMapper {
    @Override
    protected void onMapped(Object object, int id) {
      mDOMProvider.verifyThreadAccess();

      NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(object);
      descriptor.hook(object);
    }

    @Override
    protected void onUnmapped(Object object, int id) {
      mDOMProvider.verifyThreadAccess();

      NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(object);
      descriptor.unhook(object);
    }
  }

  private final class ProviderListener implements DOMProvider.Listener {
    @Override
    public void onPossiblyChanged() {
      updateTree();
    }

    @Override
    public void onAttributeModified(Object element, String name, String value) {
      mDOMProvider.verifyThreadAccess();

      AttributeModifiedEvent message = new AttributeModifiedEvent();
      message.nodeId = mObjectIdMapper.getIdForObject(element);
      message.name = name;
      message.value = value;
      mPeerManager.sendNotificationToPeers("DOM.attributeModified", message);
    }

    @Override
    public void onAttributeRemoved(Object element, String name) {
      mDOMProvider.verifyThreadAccess();

      AttributeRemovedEvent message = new AttributeRemovedEvent();
      message.nodeId = mObjectIdMapper.getIdForObject(element);
      message.name = name;
      mPeerManager.sendNotificationToPeers("DOM.attributeRemoved", message);
    }

    @Override
    public void onInspectRequested(Object element) {
      mDOMProvider.verifyThreadAccess();

      Integer nodeId = mObjectIdMapper.getIdForObject(element);
      if (nodeId == null) {
        LogUtil.d(
            "DOMProvider.Listener.onInspectRequested() called for a non-mapped node: element=%s",
            element);
      } else {
        InspectNodeRequestedEvent message = new InspectNodeRequestedEvent();
        message.nodeId = nodeId;
        mPeerManager.sendNotificationToPeers("DOM.inspectNodeRequested", message);
      }
    }
  }

  private static class GetDocumentResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public Node root;
  }

  private static class Node implements JsonRpcResult {
    @JsonProperty(required = true)
    public int nodeId;

    @JsonProperty(required = true)
    public NodeType nodeType;

    @JsonProperty(required = true)
    public String nodeName;

    @JsonProperty(required = true)
    public String localName;

    @JsonProperty(required = true)
    public String nodeValue;

    @JsonProperty
    public Integer childNodeCount;

    @JsonProperty
    public List<Node> children;

    @JsonProperty
    public List<String> attributes;
  }

  private static class AttributeModifiedEvent {
    @JsonProperty(required = true)
    public int nodeId;

    @JsonProperty(required = true)
    public String name;

    @JsonProperty(required = true)
    public String value;
  }

  private static class AttributeRemovedEvent {
    @JsonProperty(required = true)
    public int nodeId;

    @JsonProperty(required = true)
    public String name;
  }

  private static class ChildNodeInsertedEvent {
    @JsonProperty(required = true)
    public int parentNodeId;

    @JsonProperty(required = true)
    public int previousNodeId;

    @JsonProperty(required = true)
    public Node node;
  }

  private static class ChildNodeRemovedEvent {
    @JsonProperty(required = true)
    public int parentNodeId;

    @JsonProperty(required = true)
    public int nodeId;
  }

  private static class HighlightNodeRequest {
    @JsonProperty(required = true)
    public HighlightConfig highlightConfig;

    @JsonProperty
    public Integer nodeId;

    @JsonProperty
    public String objectId;
  }

  private static class HighlightConfig {
    @JsonProperty
    public RGBAColor contentColor;
  }

  private static class InspectNodeRequestedEvent {
    @JsonProperty
    public int nodeId;
  }

  private static class SetInspectModeEnabledRequest {
    @JsonProperty(required = true)
    public boolean enabled;

    @JsonProperty
    public Boolean inspectShadowDOM;

    @JsonProperty
    public HighlightConfig highlightConfig;
  }

  private static class RGBAColor {
    @JsonProperty(required = true)
    public int r;

    @JsonProperty(required = true)
    public int g;

    @JsonProperty(required = true)
    public int b;

    @JsonProperty
    public Double a;

    public int getColor() {
      byte alpha;
      if (this.a == null) {
        alpha = (byte)255;
      } else {
        long aLong = Math.round(this.a * 255.0);
        alpha = (aLong < 0) ? (byte)0 : (aLong >= 255) ? (byte)255 : (byte)aLong;
      }

      return Color.argb(alpha, this.r, this.g, this.b);
    }
  }

  private static class ResolveNodeRequest {
    @JsonProperty(required = true)
    public int nodeId;

    @JsonProperty
    public String objectGroup;
  }

  private static class SetAttributesAsTextRequest {
    @JsonProperty(required = true)
    public int nodeId;

    @JsonProperty(required = true)
    public String text;
  }

  private static class ResolveNodeResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public Runtime.RemoteObject object;
  }
}

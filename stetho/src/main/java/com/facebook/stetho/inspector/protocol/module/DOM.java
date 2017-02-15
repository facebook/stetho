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
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ArrayListAccumulator;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.UncheckedCallable;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.DocumentView;
import com.facebook.stetho.inspector.elements.Document;
import com.facebook.stetho.inspector.elements.NodeInfo;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.NodeID;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DOM implements ChromeDevtoolsDomain {
  private final ObjectMapper mObjectMapper;
  private final Document mDocument;
  private final Map<String, List<Integer>> mSearchResults;
  private final AtomicInteger mResultCounter;
  private final ChromePeerManager mPeerManager;
  private final DocumentUpdateListener mListener;

  private ChildNodeRemovedEvent mCachedChildNodeRemovedEvent;
  private ChildNodeInsertedEvent mCachedChildNodeInsertedEvent;

  public DOM(Document document) {
    mObjectMapper = new ObjectMapper();
    mDocument = Util.throwIfNull(document);
    mSearchResults = Collections.synchronizedMap(
      new HashMap<String, List<Integer>>());
    mResultCounter = new AtomicInteger(0);
    mPeerManager = new ChromePeerManager();
    mPeerManager.setListener(new PeerManagerListener());
    mListener = new DocumentUpdateListener();
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

    result.root = mDocument.postAndWait(new UncheckedCallable<Node>() {
      @Override
      public Node call() {
        Object element = mDocument.getRootElement();
        return createNodeForElement(element, mDocument.getDocumentView(), null);
      }
    });

    return result;
  }

  @ChromeDevtoolsMethod
  public void highlightNode(JsonRpcPeer peer, JSONObject params) {
    final HighlightNodeRequest request =
      mObjectMapper.convertValue(params, HighlightNodeRequest.class);
    if (request.nodeId == null) {
      LogUtil.w("DOM.highlightNode was not given a nodeId; JS objectId is not supported");
      return;
    }

    final RGBAColor contentColor = request.highlightConfig.contentColor;
    if (contentColor == null) {
      LogUtil.w("DOM.highlightNode was not given a color to highlight with");
      return;
    }

    mDocument.postAndWait(new Runnable() {
      @Override
      public void run() {
        Object element = mDocument.getElementForNodeId(request.nodeId);
        if (element != null) {
          mDocument.highlightElement(element, contentColor.getColor());
        }
      }
    });
  }

  @ChromeDevtoolsMethod
  public void hideHighlight(JsonRpcPeer peer, JSONObject params) {
    mDocument.postAndWait(new Runnable() {
      @Override
      public void run() {
        mDocument.hideHighlight();
      }
    });
  }

  @ChromeDevtoolsMethod
  public ResolveNodeResponse resolveNode(JsonRpcPeer peer, JSONObject params)
      throws JsonRpcException {
    final ResolveNodeRequest request = mObjectMapper.convertValue(params, ResolveNodeRequest.class);

    final Object element = mDocument.postAndWait(new UncheckedCallable<Object>() {
      @Override
      public Object call() {
        return mDocument.getElementForNodeId(request.nodeId);
      }
    });

    if (element == null) {
      throw new JsonRpcException(
          new JsonRpcError(
              JsonRpcError.ErrorCode.INVALID_PARAMS,
              "No known nodeId=" + request.nodeId,
              null /* data */));
    }

    int mappedObjectId = Runtime.mapObject(peer, element);

    Runtime.RemoteObject remoteObject = new Runtime.RemoteObject();
    remoteObject.type = Runtime.ObjectType.OBJECT;
    remoteObject.subtype = Runtime.ObjectSubType.NODE;
    remoteObject.className = element.getClass().getName();
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
        params,
        SetAttributesAsTextRequest.class);

    mDocument.postAndWait(new Runnable() {
      @Override
      public void run() {
        Object element = mDocument.getElementForNodeId(request.nodeId);
        if (element != null) {
          mDocument.setAttributesAsText(element, request.text);
        }
      }
    });
  }

  @ChromeDevtoolsMethod
  public void setInspectModeEnabled(JsonRpcPeer peer, JSONObject params) {
    final SetInspectModeEnabledRequest request = mObjectMapper.convertValue(
        params,
        SetInspectModeEnabledRequest.class);

    mDocument.postAndWait(new Runnable() {
      @Override
      public void run() {
        mDocument.setInspectModeEnabled(request.enabled);
      }
    });
  }

  @ChromeDevtoolsMethod
  public PerformSearchResponse performSearch(JsonRpcPeer peer, final JSONObject params) {
    final PerformSearchRequest request = mObjectMapper.convertValue(
        params,
        PerformSearchRequest.class);

    final ArrayListAccumulator<Integer> resultNodeIds = new ArrayListAccumulator<>();

    mDocument.postAndWait(new Runnable() {
      @Override
      public void run() {
        mDocument.findMatchingElements(request.query, new Accumulator<NodeID>() {
          @Override
          public void store(NodeID nodeID) {
            resultNodeIds.store(nodeID.value);
          }
        });
      }
    });

    // Each search action has a unique ID so that
    // it can be queried later.
    final String searchId = String.valueOf(mResultCounter.getAndIncrement());

    mSearchResults.put(searchId, resultNodeIds);

    final PerformSearchResponse response = new PerformSearchResponse();
    response.searchId = searchId;
    response.resultCount = resultNodeIds.size();

    return response;
  }

  @ChromeDevtoolsMethod
  public GetSearchResultsResponse getSearchResults(JsonRpcPeer peer, JSONObject params) {
    final GetSearchResultsRequest request = mObjectMapper.convertValue(
        params,
        GetSearchResultsRequest.class);

    if (request.searchId == null) {
      LogUtil.w("searchId may not be null");
      return null;
    }

    final List<Integer> results = mSearchResults.get(request.searchId);

    if (results == null) {
      LogUtil.w("\"" + request.searchId + "\" is not a valid reference to a search result");
      return null;
    }

    final List<Integer> resultsRange = results.subList(request.fromIndex, request.toIndex);

    final GetSearchResultsResponse response = new GetSearchResultsResponse();
    response.nodeIds = resultsRange;

    return response;
  }

  @ChromeDevtoolsMethod
  public void discardSearchResults(JsonRpcPeer peer, JSONObject params) {
    final DiscardSearchResultsRequest request = mObjectMapper.convertValue(
      params,
      DiscardSearchResultsRequest.class);

    if (request.searchId != null) {
      mSearchResults.remove(request.searchId);
    }
  }

  private Node createNodeForElement(
      Object element,
      DocumentView view,
      @Nullable Accumulator<Object> processedElements) {
    if (processedElements != null) {
      processedElements.store(element);
    }

    NodeDescriptor descriptor = mDocument.getNodeDescriptor(element);

    Node node = new DOM.Node();
    final NodeID nodeID = mDocument.getNodeIdForElement(element);
    node.nodeId = nodeID.value;
    node.nodeType = descriptor.getNodeType(element);
    node.nodeName = descriptor.getNodeName(element);
    node.localName = descriptor.getLocalName(element);
    node.nodeValue = descriptor.getNodeValue(element);

    Document.AttributeListAccumulator accumulator = new Document.AttributeListAccumulator();
    descriptor.getAttributes(element, accumulator);

    // Attributes
    node.attributes = accumulator;

    // Children
    NodeInfo nodeInfo = view.getNodeInfo(nodeID);
    List<Node> childrenNodes = (nodeInfo.childrenIDs.size() == 0)
        ? Collections.<Node>emptyList()
        : new ArrayList<Node>(nodeInfo.childrenIDs.size());

    for (int i = 0, N = nodeInfo.childrenIDs.size(); i < N; ++i) {
      final NodeID childID = nodeInfo.childrenIDs.get(i);
      final Object childElement = mDocument.getElementForNodeId(childID);
      Node childNode = createNodeForElement(childElement, view, processedElements);
      childrenNodes.add(childNode);
    }

    node.children = childrenNodes;
    node.childNodeCount = childrenNodes.size();

    return node;
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

  private final class DocumentUpdateListener implements Document.UpdateListener {
    public void onAttributeModified(Object element, String name, String value) {
      AttributeModifiedEvent message = new AttributeModifiedEvent();
      message.nodeId = mDocument.getNodeIdForElement(element).value;
      message.name = name;
      message.value = value;
      mPeerManager.sendNotificationToPeers("DOM.onAttributeModified", message);
    }

    public void onAttributeRemoved(Object element, String name) {
      AttributeRemovedEvent message = new AttributeRemovedEvent();
      message.nodeId = mDocument.getNodeIdForElement(element).value;
      message.name = name;
      mPeerManager.sendNotificationToPeers("DOM.attributeRemoved", message);
    }

    public void onInspectRequested(Object element) {
      NodeID nodeID = mDocument.getNodeIdForElement(element);
      if (nodeID == null) {
        LogUtil.d(
            "DocumentProvider.Listener.onInspectRequested() " +
                "called for a non-mapped nodeID: nodeID=%s",
            element);
      } else {
        InspectNodeRequestedEvent message = new InspectNodeRequestedEvent();
        message.nodeId = nodeID.value;
        mPeerManager.sendNotificationToPeers("DOM.inspectNodeRequested", message);
      }
    }

    public void onChildNodeRemoved(
        NodeID parentNodeId,
        NodeID nodeId) {
      ChildNodeRemovedEvent removedEvent = acquireChildNodeRemovedEvent();

      removedEvent.parentNodeId = parentNodeId.value;
      removedEvent.nodeId = nodeId.value;
      mPeerManager.sendNotificationToPeers("DOM.childNodeRemoved", removedEvent);

      releaseChildNodeRemovedEvent(removedEvent);
    }

    public void onChildNodeInserted(
        DocumentView view,
        Object element,
        NodeID parentNodeId,
        NodeID previousNodeId,
        Accumulator<Object> insertedElements) {
      ChildNodeInsertedEvent insertedEvent = acquireChildNodeInsertedEvent();

      insertedEvent.parentNodeId = parentNodeId.value;
      insertedEvent.previousNodeId = previousNodeId.value;
      insertedEvent.node = createNodeForElement(element, view, insertedElements);

      mPeerManager.sendNotificationToPeers("DOM.childNodeInserted", insertedEvent);

      releaseChildNodeInsertedEvent(insertedEvent);
    }
  }

  private final class PeerManagerListener extends PeersRegisteredListener {
    @Override
    protected synchronized void onFirstPeerRegistered() {
      mDocument.addRef();
      mDocument.addUpdateListener(mListener);
    }

    @Override
    protected synchronized void onLastPeerUnregistered() {
      mSearchResults.clear();
      mDocument.removeUpdateListener(mListener);
      mDocument.release();
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

  private static class PerformSearchRequest {
    @JsonProperty(required = true)
    public String query;

    @JsonProperty
    public Boolean includeUserAgentShadowDOM;
  }

  private static class PerformSearchResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public String searchId;

    @JsonProperty(required = true)
    public int resultCount;
  }

  private static class GetSearchResultsRequest {
    @JsonProperty(required = true)
    public String searchId;

    @JsonProperty(required = true)
    public int fromIndex;

    @JsonProperty(required = true)
    public int toIndex;
  }

  private static class GetSearchResultsResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public List<Integer> nodeIds;
  }

  private static class DiscardSearchResultsRequest {
    @JsonProperty(required = true)
    public String searchId;
  }
}

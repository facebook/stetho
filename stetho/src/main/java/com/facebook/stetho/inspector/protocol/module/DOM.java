/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
//
// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.protocol.module;

import android.graphics.Color;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.DOMProvider;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.ObjectIdMapper;
import com.facebook.stetho.inspector.helper.PeersRegisteredListener;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public class DOM implements ChromeDevtoolsDomain {
  private final ChromePeerManager mPeerManager;
  private final DOMProvider.Factory mDOMProviderFactory;
  private final ObjectMapper mObjectMapper;
  private final DOMObjectIdMapper mObjectIdMapper;

  @Nullable
  private DOMProvider mDOMProvider;

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
    Object rootElement = mDOMProvider.getRootElement();
    if (rootElement == null) {
      return null;
    }

    Node rootNode = createNodeForElement(rootElement);

    GetDocumentResponse result = new GetDocumentResponse();
    result.root = rootNode;
    return result;
  }

  @ChromeDevtoolsMethod
  public void highlightNode(JsonRpcPeer peer, JSONObject params) {
    HighlightNodeRequest request = mObjectMapper.convertValue(params, HighlightNodeRequest.class);
    if (request.nodeId == null) {
      LogUtil.w("DOM.highlightNode was not given a nodeId; JS objectId is not supported");
    } else {
      RGBAColor contentColor = request.highlightConfig.contentColor;
      if (contentColor == null) {
        LogUtil.w("DOM.highlightNode was not given a color to highlight with");
      } else {
        Object element = mObjectIdMapper.getObjectForId(request.nodeId);
        mDOMProvider.highlightElement(element, contentColor.getColor());
      }
    }
  }

  @ChromeDevtoolsMethod
  public void hideHighlight(JsonRpcPeer peer, JSONObject params) {
    mDOMProvider.hideHighlight();
  }

  private Node createNodeForElement(Object element) {
    NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(element);

    Node node = new Node();
    node.nodeId = mObjectIdMapper.putObject(element);
    node.nodeType = descriptor.getNodeType(element);
    node.nodeName = descriptor.getNodeName(element);
    node.localName = descriptor.getLocalName(element);
    node.nodeValue = descriptor.getNodeValue(element);

    node.children = getChildNodesForElement(element);
    node.childNodeCount = node.children.size();

    node.attributes = new ArrayList<String>();
    descriptor.copyAttributes(element, new AttributeListAccumulator(node.attributes));

    return node;
  }

  private List<Node> getChildNodesForElement(Object element) {
    NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(element);
    int childNodeCount = descriptor.getChildCount(element);

    List<Node> childNodes;
    if (childNodeCount == 0) {
      childNodes = Collections.emptyList();
    } else {
      childNodes = new ArrayList<Node>(childNodeCount);
      for (int i = 0; i < childNodeCount; ++i) {
        Object childElement = descriptor.getChildAt(element, i);
        Node childNode = createNodeForElement(childElement);
        childNodes.add(childNode);
      }
    }

    return childNodes;
  }

  private void removeElementTree(Object element) {
    NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(element);
    int childCount = descriptor.getChildCount(element);
    for (int i = 0; i < childCount; ++i) {
      Object childElement = descriptor.getChildAt(element, i);
      removeElementTree(childElement);
    }
    mObjectIdMapper.removeObject(element);
  }

  private final class PeerManagerListener extends PeersRegisteredListener {
    @Override
    protected synchronized void onFirstPeerRegistered() {
      mDOMProvider = mDOMProviderFactory.create();
      mDOMProvider.setListener(new ProviderListener());
    }

    @Override
    protected synchronized void onLastPeerUnregistered() {
      Object rootElement = mDOMProvider.getRootElement();
      removeElementTree(rootElement);

      mObjectIdMapper.clear();

      mDOMProvider.dispose();
      mDOMProvider = null;
    }
  }

  private static final class AttributeListAccumulator implements AttributeAccumulator {
    private final List<String> mList;

    public AttributeListAccumulator(List<String> list) {
      mList = Util.throwIfNull(list);
    }

    @Override
    public void add(String name, String value) {
      mList.add(name);
      mList.add(value);
    }
  }

  private final class DOMObjectIdMapper extends ObjectIdMapper {
    @Override
    protected void onMapped(Object object, int id) {
      NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(object);
      descriptor.hook(object);
    }

    @Override
    protected void onUnmapped(Object object, int id) {
      NodeDescriptor descriptor = mDOMProvider.getNodeDescriptor(object);
      descriptor.unhook(object);
    }
  }

  private final class ProviderListener implements DOMProvider.Listener {
    @Override
    public void onAttributeModified(Object element, String name, String value) {
      AttributeModifiedEvent message = new AttributeModifiedEvent();
      message.nodeId = mObjectIdMapper.getIdForObject(element);
      message.name = name;
      message.value = value;
      mPeerManager.sendNotificationToPeers("DOM.attributeModified", message);
    }

    @Override
    public void onAttributeRemoved(Object element, String name) {
      AttributeRemovedEvent message = new AttributeRemovedEvent();
      message.nodeId = mObjectIdMapper.getIdForObject(element);
      message.name = name;
      mPeerManager.sendNotificationToPeers("DOM.attributeRemoved", message);
    }

    @Override
    public void onChildInserted(Object parentElement, Object previousElement, Object childElement) {
      ChildNodeInsertedEvent message = new ChildNodeInsertedEvent();
      message.parentNodeId = mObjectIdMapper.getIdForObject(parentElement);

      // using -1 was just a guess, and it seemed to work. this is for the case where we
      // go from 0 to 1 children, or if we just happen to be inserting at index 0
      message.previousNodeId = (previousElement == null)
          ? -1
          : mObjectIdMapper.getIdForObject(previousElement);

      message.node = createNodeForElement(childElement);
      mPeerManager.sendNotificationToPeers("DOM.childNodeInserted", message);
    }

    @Override
    public void onChildRemoved(Object parentElement, Object childElement) {
      Integer parentNodeId = mObjectIdMapper.getIdForObject(parentElement);
      Integer childNodeId = mObjectIdMapper.getIdForObject(childElement);

      if (parentNodeId == null || childNodeId == null) {
        LogUtil.d(
            "DOM.onChildRemoved() called for a non-mapped node: "
                + "parentElement=(nodeId=%s, %s), childElement=(nodeId=%s, %s)",
            parentNodeId,
            parentElement,
            childNodeId,
            childElement);
      } else {
        ChildNodeRemovedEvent message = new ChildNodeRemovedEvent();
        message.parentNodeId = parentNodeId;
        message.nodeId = childNodeId;
        mPeerManager.sendNotificationToPeers("DOM.childNodeRemoved", message);
      }

      removeElementTree(childElement);
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
}

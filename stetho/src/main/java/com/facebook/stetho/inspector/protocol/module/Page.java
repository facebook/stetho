// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.protocol.module;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import com.facebook.stetho.common.ProcessUtil;
import com.facebook.stetho.inspector.console.CLog;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.annotation.JsonProperty;
import com.facebook.stetho.json.annotation.JsonValue;

import org.json.JSONObject;

public class Page implements ChromeDevtoolsDomain {
  @SuppressWarnings("unused")
  public Page(Context context) {
  }

  @ChromeDevtoolsMethod
  public void enable(JsonRpcPeer peer, JSONObject params) {
    notifyExecutionContexts(peer);
    sendWelcomeMessage(peer);
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
  }

  private void notifyExecutionContexts(JsonRpcPeer peer) {
    ExecutionContextDescription context = new ExecutionContextDescription();
    context.frameId = "1";
    context.id = 1;
    ExecutionContextCreatedParams params = new ExecutionContextCreatedParams();
    params.context = context;
    peer.invokeMethod("Runtime.executionContextCreated", params, null /* callback */);
  }

  private void sendWelcomeMessage(JsonRpcPeer peer) {
    Console.ConsoleMessage message = new Console.ConsoleMessage();
    message.source = Console.MessageSource.JAVASCRIPT;
    message.level = Console.MessageLevel.LOG;
    message.text =
// Note: not using Android resources so we can maintain .jar distribution for now.
"_____/\\\\\\\\\\\\\\\\\\\\\\_______________________________________________/\\\\\\_______________________\n" +
" ___/\\\\\\/////////\\\\\\____________________________________________\\/\\\\\\_______________________\n" +
"  __\\//\\\\\\______\\///______/\\\\\\_________________________/\\\\\\______\\/\\\\\\_______________________\n" +
"   ___\\////\\\\\\__________/\\\\\\\\\\\\\\\\\\\\\\_____/\\\\\\\\\\\\\\\\___/\\\\\\\\\\\\\\\\\\\\\\_\\/\\\\\\_____________/\\\\\\\\\\____\n" +
"    ______\\////\\\\\\______\\////\\\\\\////____/\\\\\\/////\\\\\\_\\////\\\\\\////__\\/\\\\\\\\\\\\\\\\\\\\____/\\\\\\///\\\\\\__\n" +
"     _________\\////\\\\\\______\\/\\\\\\_______/\\\\\\\\\\\\\\\\\\\\\\_____\\/\\\\\\______\\/\\\\\\/////\\\\\\__/\\\\\\__\\//\\\\\\_\n" +
"      __/\\\\\\______\\//\\\\\\_____\\/\\\\\\_/\\\\__\\//\\\\///////______\\/\\\\\\_/\\\\__\\/\\\\\\___\\/\\\\\\_\\//\\\\\\__/\\\\\\__\n" +
"       _\\///\\\\\\\\\\\\\\\\\\\\\\/______\\//\\\\\\\\\\____\\//\\\\\\\\\\\\\\\\\\\\____\\//\\\\\\\\\\___\\/\\\\\\___\\/\\\\\\__\\///\\\\\\\\\\/___\n" +
"        ___\\///////////_________\\/////______\\//////////______\\/////____\\///____\\///_____\\/////_____\n" +
"         Welcome to Stetho\n" +
"          Attached to " + ProcessUtil.getProcessName() + "\n";
    Console.MessageAddedRequest messageAddedRequest = new Console.MessageAddedRequest();
    messageAddedRequest.message = message;
    peer.invokeMethod("Console.messageAdded", messageAddedRequest, null /* callback */);
  }

  // Dog science...
  @ChromeDevtoolsMethod
  public JsonRpcResult getResourceTree(JsonRpcPeer peer, JSONObject params) {
    Frame frame = new Frame();
    frame.id = "1";
    frame.parentId = null;
    frame.loaderId = "1";
    frame.name = "Stetho";
    frame.url = "";
    frame.securityOrigin = "";
    frame.mimeType = "text/plain";
    FrameResourceTree resourceTree = new FrameResourceTree();
    resourceTree.frame = frame;
    resourceTree.resources = Collections.emptyList();
    GetResourceTreeParams resultParams = new GetResourceTreeParams();
    resultParams.frameTree = resourceTree;
    return resultParams;
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult canScreencast(JsonRpcPeer peer, JSONObject params) {
    return new SimpleBooleanResult(false);
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult hasTouchInputs(JsonRpcPeer peer, JSONObject params) {
    return new SimpleBooleanResult(false);
  }

  @ChromeDevtoolsMethod
  public void setDeviceMetricsOverride(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public void clearDeviceOrientationOverride(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public void clearGeolocationOverride(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public void setTouchEmulationEnabled(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public void setEmulatedMedia(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public void setShowViewportSizeOnResize(JsonRpcPeer peer, JSONObject params) {
  }

  private static class GetResourceTreeParams implements JsonRpcResult {
    @JsonProperty(required = true)
    public FrameResourceTree frameTree;
  }

  private static class FrameResourceTree {
    @JsonProperty(required = true)
    public Frame frame;

    public List<FrameResourceTree> childFrames;

    @JsonProperty(required = true)
    public List<Resource> resources;
  }

  private static class Frame {
    @JsonProperty(required = true)
    public String id;

    @JsonProperty
    public String parentId;

    @JsonProperty(required = true)
    public String loaderId;

    @JsonProperty
    public String name;

    @JsonProperty(required = true)
    public String url;

    @JsonProperty(required = true)
    public String securityOrigin;

    @JsonProperty(required = true)
    public String mimeType;
  }

  private static class Resource {
    // Incomplete...
  }

  public enum ResourceType {
    DOCUMENT("Document"),
    STYLESHEET("Stylesheet"),
    IMAGE("Image"),
    FONT("Font"),
    SCRIPT("Script"),
    XHR("XHR"),
    WEBSOCKET("WebSocket"),
    OTHER("Other");

    private final String mProtocolValue;

    private ResourceType(String protocolValue) {
      mProtocolValue = protocolValue;
    }

    @JsonValue
    public String getProtocolValue() {
      return mProtocolValue;
    }
  }

  private static class ExecutionContextCreatedParams {
    @JsonProperty(required = true)
    public ExecutionContextDescription context;
  }

  private static class ExecutionContextDescription {
    @JsonProperty(required = true)
    public String frameId;

    @JsonProperty(required = true)
    public int id;
  }
}

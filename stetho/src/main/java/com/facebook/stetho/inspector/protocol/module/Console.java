/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.annotation.SuppressLint;

import com.facebook.stetho.inspector.console.ConsolePeerManager;
import com.facebook.stetho.inspector.console.RuntimeReplFactory;
import com.facebook.stetho.inspector.helper.ObjectIdMapper;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcException;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcError;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;
import com.facebook.stetho.json.annotation.JsonValue;

import org.json.JSONObject;

import javax.annotation.Nullable;

public class Console implements ChromeDevtoolsDomain {
  @Nullable
  private final ObjectIdMapper mDomObjectIdMapper;
  private final RuntimeReplFactory mRuntimeReplFactory;

  private final ObjectMapper mObjectMapper = new ObjectMapper();

  /**
   * @deprecated See {@link #Console(DOM, Runtime)}
   */
  @Deprecated
  public Console() {
    mDomObjectIdMapper = null;
    mRuntimeReplFactory = null;
  }

  public Console(DOM dom, Runtime runtime) {
    mDomObjectIdMapper = dom.getObjectIdMapper();
    mRuntimeReplFactory = runtime.getReplFactory();
  }

  @ChromeDevtoolsMethod
  public void enable(JsonRpcPeer peer, JSONObject params) {
    ConsolePeerManager.getOrCreateInstance().addPeer(peer);
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
    ConsolePeerManager.getOrCreateInstance().removePeer(peer);
  }

  @ChromeDevtoolsMethod
  public void addInspectedNode(JsonRpcPeer peer, JSONObject params) throws JsonRpcException {
    if (mDomObjectIdMapper == null) {
      throw new JsonRpcException(
          new JsonRpcError(
              JsonRpcError.ErrorCode.INTERNAL_ERROR,
              "No DOM object mapper present",
              null /* data */));
    }

    AddInspectedNodeRequest request =
        mObjectMapper.convertValue(params, AddInspectedNodeRequest.class);
    Object object = mDomObjectIdMapper.getObjectForId(request.nodeId);
    if (object == null) {
      throw new JsonRpcException(
          new JsonRpcError(
              JsonRpcError.ErrorCode.INVALID_PARAMS,
              "No known nodeId=" + request.nodeId,
              null /* data */));
    }

    try {
      Runtime.addInspectedNode(peer, mRuntimeReplFactory, object);
    } catch (Throwable t) {
      throw new JsonRpcException(
          new JsonRpcError(
              JsonRpcError.ErrorCode.INTERNAL_ERROR,
              t.toString(),
              null /* data */));
    }
  }

  @SuppressLint({ "UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse" })
  public static class MessageAddedRequest {
    @JsonProperty(required = true)
    public ConsoleMessage message;
  }

  @SuppressLint({ "UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse" })
  public static class ConsoleMessage {
    @JsonProperty(required = true)
    public MessageSource source;

    @JsonProperty(required = true)
    public MessageLevel level;

    @JsonProperty(required = true)
    public String text;
  }

  public enum MessageSource {
    XML("xml"),
    JAVASCRIPT("javascript"),
    NETWORK("network"),
    CONSOLE_API("console-api"),
    STORAGE("storage"),
    APPCACHE("appcache"),
    RENDERING("rendering"),
    CSS("css"),
    SECURITY("security"),
    OTHER("other");

    private final String mProtocolValue;

    private MessageSource(String protocolValue) {
      mProtocolValue = protocolValue;
    }

    @JsonValue
    public String getProtocolValue() {
      return mProtocolValue;
    }
  }

  public enum MessageLevel {
    LOG("log"),
    WARNING("warning"),
    ERROR("error"),
    DEBUG("debug");

    private final String mProtocolValue;

    private MessageLevel(String protocolValue) {
      mProtocolValue = protocolValue;
    }

    @JsonValue
    public String getProtocolValue() {
      return mProtocolValue;
    }
  }

  @SuppressLint({ "UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse" })
  public static class CallFrame {
    @JsonProperty(required = true)
    public String functionName;

    @JsonProperty(required = true)
    public String url;

    @JsonProperty(required = true)
    public int lineNumber;

    @JsonProperty(required = true)
    public int columnNumber;

    public CallFrame() {
    }

    public CallFrame(String functionName, String url, int lineNumber, int columnNumber) {
      this.functionName = functionName;
      this.url = url;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }
  }

  private static class AddInspectedNodeRequest {
    @JsonProperty(required = true)
    public int nodeId;
  }
}

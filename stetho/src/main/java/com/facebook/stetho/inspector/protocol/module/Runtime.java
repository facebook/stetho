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

import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;

import com.facebook.stetho.json.annotation.JsonProperty;
import com.facebook.stetho.json.annotation.JsonValue;
import org.json.JSONObject;

public class Runtime implements ChromeDevtoolsDomain {
  public Runtime() {
  }

  @ChromeDevtoolsMethod
  public void releaseObjectGroup(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult evaluate(JsonRpcPeer peer, JSONObject params) {
    RemoteObject remoteObject = new RemoteObject();
    remoteObject.type = ObjectType.STRING;
    remoteObject.value = "Not supported";
    EvaluateResponse response = new EvaluateResponse();
    response.result = remoteObject;
    response.wasThrown = false;
    return response;
  }

  private static class EvaluateResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public RemoteObject result;

    @JsonProperty(required = true)
    public boolean wasThrown;
  }

  private static class RemoteObject {
    @JsonProperty(required = true)
    public ObjectType type;

    @JsonProperty
    public String value;
  }

  public enum ObjectType {
    BOOLEAN("boolean"),
    FUNCTION("function"),
    NUMBER("number"),
    OBJECT("object"),
    STRING("string"),
    UNDEFINED("undefined");

    private final String mProtocolValue;

    private ObjectType(String protocolValue) {
      mProtocolValue = protocolValue;
    }

    @JsonValue
    public String getProtocolValue() {
      return mProtocolValue;
    }
  }
}

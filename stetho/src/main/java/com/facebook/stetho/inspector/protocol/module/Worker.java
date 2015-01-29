// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.protocol.module;

import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;

import org.json.JSONObject;

public class Worker implements ChromeDevtoolsDomain {
  public Worker() {
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult canInspectWorkers(JsonRpcPeer peer, JSONObject params) {
    return new SimpleBooleanResult(true);
  }
}

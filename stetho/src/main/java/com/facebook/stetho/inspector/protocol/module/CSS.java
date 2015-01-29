// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.protocol.module;

import java.util.Collections;
import java.util.List;

import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.annotation.JsonProperty;

import org.json.JSONObject;

public class CSS implements ChromeDevtoolsDomain {
  public CSS() {
  }

  @ChromeDevtoolsMethod
  public void enable(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult getSupportedCSSProperties(JsonRpcPeer peer, JSONObject params) {
    GetSupportedCSSPropertiesResponse response = new GetSupportedCSSPropertiesResponse();
    response.cssProperties = Collections.emptyList();
    return response;
  }

  private static class GetSupportedCSSPropertiesResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public List<CSSPropertyInfo> cssProperties;
  }

  private static class CSSPropertyInfo {
    // Incomplete
  }
}

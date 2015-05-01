/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

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

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.jsonrpc.protocol;

import android.annotation.SuppressLint;

import com.facebook.stetho.json.annotation.JsonProperty;
import org.json.JSONObject;

@SuppressLint({ "UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse" })
public class JsonRpcRequest {
  /**
   * This field is not required so that we can support JSON-RPC "notification" requests.
   */
  @JsonProperty
  public Long id;

  @JsonProperty(required = true)
  public String method;

  @JsonProperty
  public JSONObject params;

  public JsonRpcRequest() {
  }

  public JsonRpcRequest(Long id, String method, JSONObject params) {
    this.id = id;
    this.method = method;
    this.params = params;
  }
}

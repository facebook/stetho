/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.jsonrpc.protocol;

import javax.annotation.Nullable;

import android.annotation.SuppressLint;

import com.facebook.stetho.json.annotation.JsonProperty;
import org.json.JSONObject;

@SuppressLint({ "UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse" })
public class JsonRpcEvent {
  @JsonProperty(required = true)
  public String method;

  @JsonProperty
  public JSONObject params;

  public JsonRpcEvent() {
  }

  public JsonRpcEvent(String method, @Nullable JSONObject params) {
    this.method = method;
    this.params = params;
  }
}

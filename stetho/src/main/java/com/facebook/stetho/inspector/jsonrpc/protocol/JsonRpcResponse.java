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
public class JsonRpcResponse {
  @JsonProperty(required = true)
  public long id;

  @JsonProperty
  public JSONObject result;

  @JsonProperty
  public JSONObject error;
}

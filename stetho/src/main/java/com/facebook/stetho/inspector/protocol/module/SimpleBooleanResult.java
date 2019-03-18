/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.protocol.module;

import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.json.annotation.JsonProperty;

public class SimpleBooleanResult implements JsonRpcResult {
  @JsonProperty(required = true)
  public boolean result;

  public SimpleBooleanResult() {
  }

  public SimpleBooleanResult(boolean result) {
    this.result = result;
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.jsonrpc;

import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcError;
import com.facebook.stetho.common.Util;

public class JsonRpcException extends Exception {
  private final JsonRpcError mErrorMessage;

  public JsonRpcException(JsonRpcError errorMessage) {
    super(errorMessage.code + ": " + errorMessage.message);
    mErrorMessage = Util.throwIfNull(errorMessage);
  }

  public JsonRpcError getErrorMessage() {
    return mErrorMessage;
  }
}

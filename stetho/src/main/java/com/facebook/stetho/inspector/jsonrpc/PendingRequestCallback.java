/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.jsonrpc;

import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcResponse;

public interface PendingRequestCallback {
  void onResponse(JsonRpcPeer peer, JsonRpcResponse response);
}

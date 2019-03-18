/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.jsonrpc;

import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcResponse;

/**
 * Marker interface used to denote a JSON-RPC result.  After conversion from Jackson,
 * this will be placed into {@link JsonRpcResponse#result}.
 */
public interface JsonRpcResult {
}

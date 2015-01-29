// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.jsonrpc;

import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcResponse;

/**
 * Marker interface used to denote a JSON-RPC result.  After conversion from Jackson,
 * this will be placed into {@link JsonRpcResponse#result}.
 */
public interface JsonRpcResult {
}

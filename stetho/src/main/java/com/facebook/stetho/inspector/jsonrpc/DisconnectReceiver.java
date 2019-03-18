/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.jsonrpc;

/**
 * @see JsonRpcPeer#registerDisconnectReceiver(DisconnectReceiver)
 */
public interface DisconnectReceiver {
  /**
   * Invoked when a WebSocket peer disconnects.
   */
  void onDisconnect();
}

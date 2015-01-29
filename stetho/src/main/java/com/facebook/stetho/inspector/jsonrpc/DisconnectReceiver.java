// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.jsonrpc;

/**
 * @see JsonRpcPeer#registerDisconnectReceiver(DisconnectReceiver)
 */
public interface DisconnectReceiver {
  /**
   * Invoked when a WebSocket peer disconnects.
   */
  public void onDisconnect();
}

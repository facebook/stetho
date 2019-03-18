/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.helper;

import java.util.concurrent.atomic.AtomicInteger;

import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;

public abstract class PeersRegisteredListener implements PeerRegistrationListener {
  private AtomicInteger mPeers = new AtomicInteger(0);

  @Override
  public final void onPeerRegistered(JsonRpcPeer peer) {
    if (mPeers.incrementAndGet() == 1) {
      onFirstPeerRegistered();
    }
    onPeerAdded(peer);
  }

  @Override
  public final void onPeerUnregistered(JsonRpcPeer peer) {
    if (mPeers.decrementAndGet() == 0) {
      onLastPeerUnregistered();
    }
    onPeerRemoved(peer);
  }

  protected void onPeerAdded(JsonRpcPeer peer) {}
  protected void onPeerRemoved(JsonRpcPeer peer) {}

  protected abstract void onFirstPeerRegistered();
  protected abstract void onLastPeerUnregistered();
}

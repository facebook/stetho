/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
//
// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.helper;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.facebook.stetho.common.LogRedirector;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.jsonrpc.DisconnectReceiver;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.PendingRequestCallback;

/**
 * Interface glue that allows a particular domain to manage the enabled peers.  The way the
 * WebKit inspector protocol works is that each functionality domain has an enable/disable JSON-RPC
 * method call which alerts the server (that's us) that we can now begin sending local events
 * to the peer to have them appear in the inspector UI.  This class simplifies managing those
 * enabled peers for each functionality domain.
 */
public abstract class ChromePeerManager {
  private static final String TAG = "ChromePeerManager";

  /**
   * Set of registered peers, mapped to the disconnect receiver for automatic unregistration
   * purposes.
   */
  @GuardedBy("this")
  private final Map<JsonRpcPeer, DisconnectReceiver> mReceivingPeers =
      new HashMap<JsonRpcPeer, DisconnectReceiver>();

  @GuardedBy("this")
  private PeerRegistrationListener mListener;

  public ChromePeerManager() {
  }

  /**
   * Set a listener which can receive notifications of unique registration event (see
   * {@link #addPeer} and {@link #removePeer}).
   *
   * @param listener
   */
  public synchronized void setListener(PeerRegistrationListener listener) {
    mListener = listener;
  }

  /**
   * Register a new peer, adding them to an internal list of receivers.
   *
   * @param peer
   * @return True if this is a newly registered peer; false if it was already registered.
   */
  public synchronized boolean addPeer(JsonRpcPeer peer) {
    if (mReceivingPeers.containsKey(peer)) {
      return false;
    }

    DisconnectReceiver disconnectReceiver = new UnregisterOnDisconnect(peer);
    peer.registerDisconnectReceiver(disconnectReceiver);
    mReceivingPeers.put(peer, disconnectReceiver);
    if (mListener != null) {
      mListener.onPeerRegistered(peer);
    }
    return true;
  }

  /**
   * Unregister an existing peer.
   *
   * @param peer
   */
  public synchronized void removePeer(JsonRpcPeer peer) {
    if (mReceivingPeers.remove(peer) != null) {
      if (mListener != null) {
        mListener.onPeerUnregistered(peer);
      }
    }
  }

  public synchronized boolean hasRegisteredPeers() {
    return !mReceivingPeers.isEmpty();
  }

  private synchronized ArrayList<JsonRpcPeer> getReceivingPeersCopy() {
    // This performance should be OK in practice because we rarely have more than 1 peer connected.
    return new ArrayList<JsonRpcPeer>(mReceivingPeers.keySet());
  }

  public void sendNotificationToPeers(String method,
      Object params) {
    sendMessageToPeers(method, params, null /* callback */);
  }

  public void invokeMethodOnPeers(String method,
      Object params,
      PendingRequestCallback callback) {
    Util.throwIfNull(callback);
    sendMessageToPeers(method, params, callback);
  }

  private void sendMessageToPeers(String method,
      Object params,
      @Nullable PendingRequestCallback callback) {
    ArrayList<JsonRpcPeer> peersCopy = getReceivingPeersCopy();
    int peersCopyN = peersCopy.size();
    for (int i = 0; i < peersCopyN; i++) {
      JsonRpcPeer peer = peersCopy.get(i);
      try {
        peer.invokeMethod(method, params, callback);
      } catch (NotYetConnectedException e) {
        LogRedirector.e(TAG, "Error delivering data to Chrome", e);
      }
    }
  }

  private class UnregisterOnDisconnect implements DisconnectReceiver {
    private final JsonRpcPeer mPeer;

    public UnregisterOnDisconnect(JsonRpcPeer peer) {
      mPeer = peer;
    }

    @Override
    public void onDisconnect() {
      removePeer(mPeer);
    }
  }
}

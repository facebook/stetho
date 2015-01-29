// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.helper;

import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;

public interface PeerRegistrationListener {
  public void onPeerRegistered(JsonRpcPeer peer);
  public void onPeerUnregistered(JsonRpcPeer peer);
}

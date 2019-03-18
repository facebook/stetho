/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.helper;

import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;

public interface PeerRegistrationListener {
  void onPeerRegistered(JsonRpcPeer peer);
  void onPeerUnregistered(JsonRpcPeer peer);
}

// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.websocket;

interface ReadCallback {
  public void onCompleteFrame(byte opcode, byte[] payload, int payloadLen);
}

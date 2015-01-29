// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.network;

/**
 * Special file data necessary to comply with the Chrome DevTools instance which doesn't let
 * us just naively base64 encode everything.
 */
public class ResponseBodyData {
  public String data;
  public boolean base64Encoded;
}

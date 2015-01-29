// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.websocket;

/**
 * Close codes as defined by RFC6455.
 */
public interface CloseCodes {
  public static final int NORMAL_CLOSURE = 1000;
  public static final int PROTOCOL_ERROR = 1002;
  public static final int CLOSED_ABNORMALLY = 1006;
  public static final int UNEXPECTED_CONDITION = 1011;
}

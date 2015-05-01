/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

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

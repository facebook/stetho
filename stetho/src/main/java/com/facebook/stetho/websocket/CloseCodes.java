/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.websocket;

/**
 * Close codes as defined by RFC6455.
 */
public interface CloseCodes {
  int NORMAL_CLOSURE = 1000;
  int PROTOCOL_ERROR = 1002;
  int CLOSED_ABNORMALLY = 1006;
  int UNEXPECTED_CONDITION = 1011;
}

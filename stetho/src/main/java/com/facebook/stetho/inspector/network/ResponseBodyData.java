/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

/**
 * Special file data necessary to comply with the Chrome DevTools instance which doesn't let
 * us just naively base64 encode everything.
 */
public class ResponseBodyData {
  public String data;
  public boolean base64Encoded;
}

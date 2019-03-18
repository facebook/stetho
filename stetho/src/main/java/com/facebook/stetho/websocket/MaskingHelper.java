/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.websocket;

class MaskingHelper {
  public static void unmask(byte[] key, byte[] data, int offset, int count) {
    int index = 0;
    while (count-- > 0) {
      data[offset++] ^= key[index++ % key.length];
    }
  }
}

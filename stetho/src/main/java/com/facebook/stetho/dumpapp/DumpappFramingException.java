/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.dumpapp;

import java.io.IOException;

/**
 * Thrown to indicate an error in the dumpapp framing protocol as received from the remote
 * peer.
 */
class DumpappFramingException extends IOException {
  public DumpappFramingException(String message) {
    super(message);
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.server;

import android.net.LocalSocket;
import com.facebook.stetho.server.CompositeInputStream;
import com.facebook.stetho.server.LeakyBufferedInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility to allow reading buffered data from a socket and then "unreading" the data
 * and combining it with the original unbuffered stream.  This is useful when
 * handing off from one logical protocol layer to the next, such as when upgrading an HTTP
 * connection to the websocket protocol.
 */
public class SocketLike {
  private final LocalSocket mSocket;
  private final LeakyBufferedInputStream mLeakyInput;

  public SocketLike(SocketLike socketLike, LeakyBufferedInputStream leakyInput) {
    this(socketLike.mSocket, leakyInput);
  }

  public SocketLike(LocalSocket socket, LeakyBufferedInputStream leakyInput) {
    mSocket = socket;
    mLeakyInput = leakyInput;
  }

  public InputStream getInput() throws IOException {
    return mLeakyInput.leakBufferAndStream();
  }

  public OutputStream getOutput() throws IOException {
    return mSocket.getOutputStream();
  }
}

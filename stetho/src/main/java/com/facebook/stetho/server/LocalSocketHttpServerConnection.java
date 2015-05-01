/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.net.LocalSocket;

import com.facebook.stetho.common.Util;

import org.apache.http.impl.AbstractHttpServerConnection;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.AbstractSessionOutputBuffer;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class LocalSocketHttpServerConnection extends AbstractHttpServerConnection {
  private volatile LocalSocket mSocket;
  private volatile boolean mOpen;
  private volatile LocalSocketSessionInputBuffer mInputBuffer;

  public void bind(LocalSocket socket, HttpParams params) throws IOException {
    Util.throwIfNull(socket);
    Util.throwIfNull(params);

    mSocket = socket;

    int bufferSize = HttpConnectionParams.getSocketBufferSize(params);

    mInputBuffer = new LocalSocketSessionInputBuffer(socket, bufferSize, params);
    init(
        mInputBuffer,
        new LocalSocketSessionOutputBuffer(socket, bufferSize, params),
        params);

    mOpen = true;
  }

  public LocalSocket getSocket() {
    return mSocket;
  }

  /**
   * Clear the input buffer and return the data that was contained there.  This is a hack
   * needed to try to upgrade to a raw socket protocol (for instance, WebSocket).
   */
  public byte[] clearInputBuffer() {
    return mInputBuffer.clearCurrentBuffer();
  }

  @Override
  protected void assertOpen() throws IllegalStateException {
    Util.throwIfNot(mOpen);
  }

  @Override
  public boolean isOpen() {
    return mOpen;
  }

  @Override
  public void setSocketTimeout(int timeout) {
    try {
      mSocket.setSoTimeout(timeout);
    } catch (IOException e) {
      Util.throwIfNot(mSocket.isClosed());
    }
  }

  @Override
  public int getSocketTimeout() {
    try {
      return mSocket.getSoTimeout();
    } catch (IOException e) {
      Util.throwIfNot(mSocket.isClosed());
      return -1;
    }
  }

  @Override
  public void shutdown() throws IOException {
    close(false /* doFlush */);
  }

  @Override
  public void close() throws IOException {
    close(true /* doFlush */);
  }

  private void close(boolean doFlush) throws IOException {
    if (!mOpen) {
      return;
    }
    mOpen = false;
    if (doFlush) {
      doFlush();
    }
    mSocket.close();
  }

  private static class LocalSocketSessionInputBuffer extends AbstractSessionInputBuffer {
    public LocalSocketSessionInputBuffer(LocalSocket socket,
        int bufferSize,
        HttpParams params) throws IOException {
      if (HttpConnectionParams.isStaleCheckingEnabled(params)) {
        throw new UnsupportedOperationException(
            "Stale connection checking should not be used for local sockets");
      }
      init(socket.getInputStream(), bufferSize, params);
    }

    public byte[] clearCurrentBuffer() {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      try {
        while (hasBufferedData()) {
          int b = read();
          Util.throwIfNot(b != -1, "Buffered data cannot EOF");
          buffer.write(b);
        }
        return buffer.toByteArray();
      } catch (IOException e) {
        // This shouldn't be possible.
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean isDataAvailable(int timeout) throws IOException {
      throw new UnsupportedOperationException(
          "CoreConnectionPNames.STALE_CONNECTION_CHECK appears to be true but that can't be?");
    }
  }

  private static class LocalSocketSessionOutputBuffer extends AbstractSessionOutputBuffer {
    public LocalSocketSessionOutputBuffer(LocalSocket socket,
        int bufferSize,
        HttpParams params) throws IOException {
      init(socket.getOutputStream(), bufferSize, params);
    }

    @Override
    public void flush() throws IOException {
      // Do not call through to super.flush() to fix a serious throughput issue due to a
      // polling/sleep based implementation of LocalSocketImpl#flush.  This is apparently done to
      // guarantee that after flush is called the write buffer has been fully drained but we don't
      // need or expect this guarantee since our buffering strategy is entirely at the HTTP
      // level.
      flushBuffer();
    }
  }
}

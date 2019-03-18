/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.sample;

import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.SimpleTextInspectorWebSocketFrame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.annotation.Nullable;

/**
 * Simple IRC client connection system to demonstrate Stetho's "websocket" (any arbitrary
 * socket will work too) support.
 */
public class IRCClientConnection implements Closeable {
  private final StethoReporter mReporter;

  private final Socket mSocket;
  private final BufferedReader mInput;
  private final BufferedWriter mOutput;

  public static IRCClientConnection connect(String host, int port) throws IOException {
    StethoReporter reporter = new StethoReporter();
    Socket socket = new Socket();
    reporter.onPreConnect(host, port);
    try {
      socket.connect(new InetSocketAddress(host, port));
      reporter.onPostConnect();
    } catch (IOException e) {
      reporter.onError(e);
      try {
        socket.close();
        throw e;
      } finally {
        reporter.onClosed();
      }
    }
    return new IRCClientConnection(reporter, socket, "UTF-8");
  }

  private IRCClientConnection(
      StethoReporter reporter,
      Socket socket,
      String charset)
      throws IOException {
    mReporter = reporter;
    mSocket = socket;
    mInput = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
    mOutput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset));
  }

  @Nullable
  public String read() throws IOException {
    try {
      String message = mInput.readLine();
      if (message != null) {
        mReporter.onReceive(message);
        maybeHandleIncomingMessage(message);
      }
      return message;
    } catch (IOException e) {
      mReporter.onError(e);
      throw e;
    }
  }

  public void send(String message) throws IOException {
    mReporter.onSend(message);
    try {
      mOutput.write(message + "\r\n");
      mOutput.flush();
    } catch (IOException e) {
      mReporter.onError(e);
      throw e;
    }
  }

  private boolean maybeHandleIncomingMessage(String message) throws IOException {
    if (message.startsWith("PING ")) {
      send("PONG " + message.substring("PING ".length()));
      return true;
    }
    return false;
  }

  public void close() throws IOException {
    try {
      try {
        mOutput.close();
      } catch (IOException e) {
        mReporter.onError(e);
        throw e;
      }
    } finally {
      try {
        mSocket.close();
      } catch (IOException e) {
        mReporter.onError(e);

        // Technically this should use addSuppressed but it's KITKAT only and this is a demo...
        throw e;
      } finally {
        mReporter.onClosed();
      }
    }
  }

  private static class StethoReporter {
    private final NetworkEventReporter mReporter;
    private final String mRequestId;

    public StethoReporter() {
      mReporter = NetworkEventReporterImpl.get();
      mRequestId = mReporter.nextRequestId();
    }

    public void onPreConnect(String host, int port) {
      mReporter.webSocketCreated(mRequestId, "irc://" + host + ":" + port);
    }

    public void onPostConnect() {
      // Sadly, nothing to report...
    }

    public void onError(IOException e) {
      mReporter.webSocketFrameError(mRequestId, e.getMessage());
    }

    public void onClosed() {
      mReporter.webSocketClosed(mRequestId);
    }

    public void onSend(String message) {
      mReporter.webSocketFrameSent(new SimpleTextInspectorWebSocketFrame(mRequestId, message));
    }

    public void onReceive(String message) {
      mReporter.webSocketFrameReceived(new SimpleTextInspectorWebSocketFrame(mRequestId, message));
    }
  }
}

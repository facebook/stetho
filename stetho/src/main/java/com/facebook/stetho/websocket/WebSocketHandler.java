/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.websocket;

import javax.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.net.LocalSocket;
import android.util.Base64;

import com.facebook.stetho.common.Utf8Charset;
import com.facebook.stetho.server.LocalSocketHttpServerConnection;
import com.facebook.stetho.server.SecureHttpRequestHandler;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;

/**
 * Crazy kludge to support upgrading to the WebSocket protocol while still using the
 * {@link HttpRequestHandler} harness.
 * <p>
 * The way this works is that we pump the request directly into our WebSocket implementation and
 * force write the response out to the connection without returning.  Then, we extract the
 * remaining buffered input stream bytes from the socket and stitch them together with the
 * raw sockets input stream and pass everything onto the WebSocket engine which blocks
 * until WebSocket orderly shutdown.  On shutdown, we force throw a
 * {@link ConnectionClosedException} to "gracefully" exit to our server harness code.
 * <p>
 * This upgrade helper approach only works if the underlying connection is of type
 * {@link LocalSocketHttpServerConnection}.  This is needed so that we have reliable access both
 * to the underlying socket and to the request input buffer which must be drained and sent to the
 * WebSocket engine.
 * <p>
 * This class is generally considered to be a fairly fragile hack on top of
 * Apache's {@link HttpService} and should not be used outside of debug code.
 */
public class WebSocketHandler extends SecureHttpRequestHandler {
  private static final String HEADER_UPGRADE = "Upgrade";
  private static final String HEADER_CONNECTION = "Connection";
  private static final String HEADER_SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
  private static final String HEADER_SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
  private static final String HEADER_SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
  private static final String HEADER_SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

  private static final String HEADER_UPGRADE_WEBSOCKET = "websocket";
  private static final String HEADER_CONNECTION_UPGRADE = "Upgrade";
  private static final String HEADER_SEC_WEBSOCKET_VERSION_13 = "13";

  // Are you kidding me?  The WebSocket spec requires that we append this weird hardcoded String
  // to the key we receive from the client, SHA-1 that, and base64 encode it back to the client.
  // I'm guessing this is to prevent replay attacks of some kind but given that there's no actual
  // security context here, I can only imagine that this is just security through obscurity in
  // some fashion.
  private static final String SERVER_KEY_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  private final SimpleEndpoint mEndpoint;

  public WebSocketHandler(Context context, SimpleEndpoint endpoint) {
    super(context);
    mEndpoint = endpoint;
  }

  @Override
  public void handleSecured(
      HttpRequest request,
      HttpResponse response,
      HttpContext context)
      throws IOException, HttpException {
    if (!isSupportableUpgradeRequest(request)) {
      response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
      response.setReasonPhrase("Not Implemented");
      response.setEntity(new StringEntity("Not a supported WebSocket upgrade request\n"));
      return;
    }

    HttpConnection conn = (HttpConnection)context.getAttribute(ExecutionContext.HTTP_CONNECTION);
    try {
      // This will not return on successful WebSocket upgrade, but rather block until the session is
      // shut down or a socket error occurs.
      doUpgrade(request, response, context);
    } finally {
      try {
        conn.close();
      } catch (IOException e) {
        // LocalSocket has strange behaviour with respect to flushing the socket output
        // stream.  In particular, it will throw if the socket has been closed even if there
        // is no data to flush.  Moreover, the socket will seemingly be automatically closed
        // when read to exhaustion even though it is not explicitly closed.
      }
    }

    // Throw on graceful shutdown (*sigh*) to signal to the server component that we need
    // to abort the HTTP stream loop we were previously stuck in.
    throw new ConnectionClosedException("EOF");
  }

  private static boolean isSupportableUpgradeRequest(HttpRequest request) {
    return HEADER_UPGRADE_WEBSOCKET.equalsIgnoreCase(getFirstHeaderValue(request, HEADER_UPGRADE)) &&
        HEADER_CONNECTION_UPGRADE.equals(getFirstHeaderValue(request, HEADER_CONNECTION)) &&
        HEADER_SEC_WEBSOCKET_VERSION_13.equals(
            getFirstHeaderValue(request, HEADER_SEC_WEBSOCKET_VERSION));
  }

  private void doUpgrade(
      HttpRequest request,
      HttpResponse response,
      HttpContext context)
      throws IOException, HttpException {
    RawSocketUpgradeHelper rawSocketHelper =
        RawSocketUpgradeHelper.fromApacheContext(context);

    response.setStatusCode(HttpStatus.SC_SWITCHING_PROTOCOLS);
    response.setReasonPhrase("Switching Protocols");
    response.addHeader(HEADER_UPGRADE, HEADER_UPGRADE_WEBSOCKET);
    response.addHeader(HEADER_CONNECTION, HEADER_CONNECTION_UPGRADE);

    String clientKey = getFirstHeaderValue(request, HEADER_SEC_WEBSOCKET_KEY);
    if (clientKey != null) {
      response.addHeader(HEADER_SEC_WEBSOCKET_ACCEPT, generateServerKey(clientKey));
    }

    forceSendResponse(rawSocketHelper.getServerConnection(), response);

    WebSocketSession session = new WebSocketSession(
        rawSocketHelper.getInputStream(),
        rawSocketHelper.getOutputStream(),
        mEndpoint);
    session.handle();
  }

  private static String generateServerKey(String clientKey) {
    try {
      String serverKey = clientKey + SERVER_KEY_GUID;
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      sha1.update(Utf8Charset.encodeUTF8(serverKey));
      return Base64.encodeToString(sha1.digest(), Base64.NO_WRAP);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private static String getFirstHeaderValue(HttpMessage message, String headerName) {
    Header header = message.getFirstHeader(headerName);
    return header != null ? header.getValue() : null;
  }

  /**
   * Force write the HTTP response outside the normal {@link HttpRequestHandler} harness mechanism.
   * This allows us to stay "stuck" in handle operation and takeover management of the socket.
   */
  private void forceSendResponse(
      HttpServerConnection conn,
      HttpResponse response)
      throws IOException, HttpException {
    conn.sendResponseHeader(response);
    conn.flush();
  }

  /**
   * Utility to upgrade a normal HTTP connection to a raw socket (for use with the WebSocket
   * implementation).  This requires that we read the previously buffered data and stitch together
   * a magic input stream
   */
  private static class RawSocketUpgradeHelper {
    private final HttpServerConnection mConn;
    private final InputStream mIn;
    private final OutputStream mOut;

    public static RawSocketUpgradeHelper fromApacheContext(HttpContext context)
        throws IOException {
      LocalSocketHttpServerConnection conn =
          (LocalSocketHttpServerConnection)context.getAttribute(ExecutionContext.HTTP_CONNECTION);
      LocalSocket socketLike = conn.getSocket();

      byte[] excessInput = conn.clearInputBuffer();

      return new RawSocketUpgradeHelper(conn,
          joinInputStreams(new ByteArrayInputStream(excessInput), socketLike.getInputStream()),
          socketLike.getOutputStream());
    }

    private RawSocketUpgradeHelper(HttpServerConnection conn,
        InputStream in,
        OutputStream out) {
      mConn = conn;
      mIn = in;
      mOut = out;
    }

    private static InputStream joinInputStreams(InputStream... streams) throws IOException {
      return new CompositeInputStream(streams);
    }

    public HttpServerConnection getServerConnection() {
      return mConn;
    }

    public InputStream getInputStream() {
      return mIn;
    }

    public OutputStream getOutputStream() {
      return mOut;
    }
  }
}

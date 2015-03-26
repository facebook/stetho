/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.urlconnection;

import com.facebook.stetho.inspector.network.DefaultResponseHandler;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Individual connection flow manager that aids in communicating network events to Stetho
 * via the {@link NetworkEventReporter} API.  This class is stateful and should be instantiated
 * for each individual HTTP request.
 * <p>
 * Be aware that there are caveats with inspection using {@link HttpURLConnection} on Android:
 * <ul>
 * <li>Compressed payload sizes are typically not available, even when compression was in use over
 * the wire.
 * <li>Redirects are by default handled internally, making it impossible to visualize them.
 * To visualize them, redirects must be handled manually by invoking
 * {@link HttpURLConnection#setFollowRedirects(boolean)}.
 * </ul>
 */
@NotThreadSafe
public class StethoURLConnectionManager {
  private static final AtomicInteger sSequenceNumberGenerator = new AtomicInteger(0);

  private final NetworkEventReporter mStethoHook = NetworkEventReporterImpl.get();
  private final int mRequestId;
  @Nullable private final String mFriendlyName;

  @Nullable private String mRequestIdString;

  private HttpURLConnection mConnection;
  @Nullable private URLConnectionInspectorRequest mInspectorRequest;

  public StethoURLConnectionManager(@Nullable String friendlyName) {
    mRequestId = sSequenceNumberGenerator.getAndIncrement();
    mFriendlyName = friendlyName;
  }

  public boolean isStethoEnabled() {
    return mStethoHook.isEnabled();
  }

  /**
   * Indicates that the {@link HttpURLConnection} instance has been configured and is about
   * to be used to initiate an actual HTTP connection.  Call this method before any of the
   * active methods such as {@link HttpURLConnection#connect()},
   * {@link HttpURLConnection#getInputStream()}, or {@link HttpURLConnection#getOutputStream()}
   *
   * @param connection Connection instance configured with a method and headers.
   * @param requestEntity Represents the request body if the request method supports it.
   */
  public void preConnect(
      HttpURLConnection connection,
      @Nullable SimpleRequestEntity requestEntity) {
    throwIfConnection();
    mConnection = connection;
    if (isStethoEnabled()) {
      mInspectorRequest = new URLConnectionInspectorRequest(
          getStethoRequestId(),
          mFriendlyName,
          connection,
          requestEntity);
      mStethoHook.requestWillBeSent(mInspectorRequest);
    }
  }

  /**
   * Indicates that the {@link HttpURLConnection} has just successfully exchanged HTTP messages
   * (request headers + body and response headers) with the server but has not yet consumed
   * the response body.
   *
   * @throws IOException May throw an exception internally due to {@link HttpURLConnection}
   *     method signatures.  The request should be considered aborted/failed if this method
   *     throws.
   */
  public void postConnect() throws IOException {
    throwIfNoConnection();
    if (isStethoEnabled()) {
      if (mInspectorRequest != null) {
        byte[] body = mInspectorRequest.body();
        if (body != null) {
          mStethoHook.dataSent(getStethoRequestId(), body.length, body.length);
        }
      }
      mStethoHook.responseHeadersReceived(
          new URLConnectionInspectorResponse(
              getStethoRequestId(),
              mConnection));
    }
  }

  /**
   * Indicates that there was a non-recoverable failure during HTTP message exchange at some
   * point between {@link #preConnect} and {@link #interpretResponseStream}.
   *
   * @param ex Relay the exception that was thrown from {@link java.net.HttpURLConnection}
   */
  public void httpExchangeFailed(IOException ex) {
    throwIfNoConnection();
    if (isStethoEnabled()) {
      mStethoHook.httpExchangeFailed(getStethoRequestId(), ex.toString());
    }
  }

  /**
   * Deliver the response stream from {@link HttpURLConnection#getInputStream()} to
   * Stetho so that it can be intercepted.  Note that compression is transparently
   * supported on modern Android systems and no special awareness is necessary for
   * gzip compression on the wire.  Unfortunately this means that it is sometimes impossible
   * to determine whether compression actually occurred and so Stetho may report inflated
   * byte counts.
   * <p>
   * If the {@code Content-Length} header is provided by the server, this will be assumed to be
   * the raw byte count on the wire.
   *
   * @param responseStream Stream as furnished by {@link HttpURLConnection#getInputStream()}.
   *
   * @return The filtering stream which is to be read after this method is called.
   */
  public InputStream interpretResponseStream(@Nullable InputStream responseStream) {
    throwIfNoConnection();
    if (isStethoEnabled()) {
      // Note that Content-Encoding is stripped out by HttpURLConnection on modern versions of
      // Android (fun fact, it's powered by okhttp) when decompression is handled transparently.
      // When this occurs, we will not be able to report the compressed size properly.  Callers,
      // however, can disable this behaviour which will once again give us access to the raw
      // Content-Encoding so that we can handle it properly.
      responseStream = mStethoHook.interpretResponseStream(
          getStethoRequestId(),
          mConnection.getHeaderField("Content-Type"),
          mConnection.getHeaderField("Content-Encoding"),
          responseStream,
          new DefaultResponseHandler(mStethoHook, getStethoRequestId()));
    }
    return responseStream;
  }

  private void throwIfNoConnection() {
    if (mConnection == null) {
      throw new IllegalStateException("Must call preConnect");
    }
  }

  private void throwIfConnection() {
    if (mConnection != null) {
      throw new IllegalStateException("Must not call preConnect twice");
    }
  }

  /**
   * Convenience method to access the lower level {@link NetworkEventReporter} API.
   */
  public NetworkEventReporter getStethoHook() {
    return mStethoHook;
  }

  /**
   * Low level method to access this request's unique identifier according to
   * {@link NetworkEventReporter}.  Most callers won't need this.
   */
  @Nonnull
  public String getStethoRequestId() {
    if (mRequestIdString == null) {
      mRequestIdString = String.valueOf(mRequestId);
    }
    return mRequestIdString;
  }
}

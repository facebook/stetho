/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.urlconnection;

import com.facebook.stetho.inspector.network.DefaultResponseHandler;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.RequestBodyHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Isolated implementation class to allow us to escape the verifier if Stetho is not
 * present.  This is done for convenience so that {@link StethoURLConnectionManager} hooks can be
 * left in a release build without any significant overhead either at runtime or in the compiled
 * APK.
 */
class StethoURLConnectionManagerImpl {
  private final NetworkEventReporter mStethoHook = NetworkEventReporterImpl.get();
  private final String mRequestId;
  @Nullable
  private final String mFriendlyName;

  private HttpURLConnection mConnection;
  @Nullable private URLConnectionInspectorRequest mInspectorRequest;
  @Nullable private RequestBodyHelper mRequestBodyHelper;

  public StethoURLConnectionManagerImpl(@Nullable String friendlyName) {
    mRequestId = mStethoHook.nextRequestId();
    mFriendlyName = friendlyName;
  }

  public boolean isStethoActive() {
    return mStethoHook.isEnabled();
  }

  /**
   * @see StethoURLConnectionManager#preConnect
   */
  public void preConnect(
      HttpURLConnection connection,
      @Nullable SimpleRequestEntity requestEntity) {
    throwIfConnection();
    mConnection = connection;
    if (isStethoActive()) {
      mRequestBodyHelper = new RequestBodyHelper(mStethoHook, getStethoRequestId());
      mInspectorRequest = new URLConnectionInspectorRequest(
          getStethoRequestId(),
          mFriendlyName,
          connection,
          requestEntity,
          mRequestBodyHelper);
      mStethoHook.requestWillBeSent(mInspectorRequest);
    }
  }

  /**
   * @see StethoURLConnectionManager#postConnect
   */
  public void postConnect() throws IOException {
    throwIfNoConnection();
    if (isStethoActive()) {
      if (mRequestBodyHelper != null && mRequestBodyHelper.hasBody()) {
        mRequestBodyHelper.reportDataSent();
      }
      mStethoHook.responseHeadersReceived(
          new URLConnectionInspectorResponse(
              getStethoRequestId(),
              mConnection));
    }
  }

  /**
   * @see StethoURLConnectionManager#httpExchangeFailed
   */
  public void httpExchangeFailed(IOException ex) {
    throwIfNoConnection();
    if (isStethoActive()) {
      mStethoHook.httpExchangeFailed(getStethoRequestId(), ex.toString());
    }
  }

  /**
   * @see StethoURLConnectionManager#interpretResponseStream
   */
  public InputStream interpretResponseStream(@Nullable InputStream responseStream) {
    throwIfNoConnection();
    if (isStethoActive()) {
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
   * @see StethoURLConnectionManager#getStethoRequestId()
   */
  @Nonnull
  public String getStethoRequestId() {
    return mRequestId;
  }
}

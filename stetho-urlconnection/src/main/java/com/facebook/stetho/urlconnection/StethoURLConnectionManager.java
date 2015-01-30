package com.facebook.stetho.urlconnection;

import com.facebook.stetho.inspector.network.DefaultResponseHandler;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.ResponseHandler;

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
   * Stetho so that it can be intercepted.  Note that this should be
   * the uncompressed stream if gzip encoded was used, so manually wrapping it in a
   * {@link java.util.zip.GZIPInputStream} would be required.  If you do this, the
   * raw encoded sizes will be incorrect by default which can be fixed by supplying
   * your own custom {@link ResponseHandler}.
   *
   * @param responseStream Stream as furnished by {@link HttpURLConnection#getInputStream()} or a
   *     decompressing one if compression was used.
   * @param customResponseHandler Custom response handler hook to allow callers to report
   *     the compressed and uncompressed sizes if desired.  This is not required and can
   *     be null.
   *
   * @return The filtering stream which is to be read after this method is called.
   */
  public InputStream interpretResponseStream(
      @Nullable InputStream responseStream,
      @Nullable ResponseHandler customResponseHandler) {
    throwIfNoConnection();
    if (isStethoEnabled()) {
      ResponseHandler responseHandler = customResponseHandler;
      if (responseHandler == null) {
        responseHandler = new DefaultResponseHandler(mStethoHook, getStethoRequestId());
      }
      responseStream = mStethoHook.interpretResponseStream(
          getStethoRequestId(),
          mConnection.getHeaderField("Content-Type"),
          responseStream,
          responseHandler);
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

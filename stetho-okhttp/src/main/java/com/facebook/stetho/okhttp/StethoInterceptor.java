/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.okhttp;

import com.facebook.stetho.inspector.network.DefaultResponseHandler;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.RequestBodyHelper;
import com.squareup.okhttp.Connection;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides easy integration with <a href="http://square.github.io/okhttp/">OkHttp</a> 2.2.0+
 * by way of the new <a href="https://github.com/square/okhttp/wiki/Interceptors">Interceptor</a>
 * system. To use:
 * <pre>
 *   OkHttpClient client = new OkHttpClient();
 *   client.networkInterceptors().add(new StethoInterceptor());
 * </pre>
 *
 * @deprecated replaced with {@code com.facebook.stetho.okhttp3.StethoInterceptor}.
 */
@Deprecated
public class StethoInterceptor implements Interceptor {
  private final NetworkEventReporter mEventReporter = NetworkEventReporterImpl.get();

  @Override
  public Response intercept(Chain chain) throws IOException {
    String requestId = mEventReporter.nextRequestId();

    Request request = chain.request();

    RequestBodyHelper requestBodyHelper = null;
    if (mEventReporter.isEnabled()) {
      requestBodyHelper = new RequestBodyHelper(mEventReporter, requestId);
      OkHttpInspectorRequest inspectorRequest =
          new OkHttpInspectorRequest(requestId, request, requestBodyHelper);
      mEventReporter.requestWillBeSent(inspectorRequest);
    }

    Response response;
    try {
      response = chain.proceed(request);
    } catch (IOException e) {
      if (mEventReporter.isEnabled()) {
        mEventReporter.httpExchangeFailed(requestId, e.toString());
      }
      throw e;
    }

    if (mEventReporter.isEnabled()) {
      if (requestBodyHelper != null && requestBodyHelper.hasBody()) {
        requestBodyHelper.reportDataSent();
      }

      Connection connection = chain.connection();
      if (connection == null) {
        throw new IllegalStateException(
            "No connection associated with this request; " +
                "did you use addInterceptor instead of addNetworkInterceptor?");
      }
      mEventReporter.responseHeadersReceived(
          new OkHttpInspectorResponse(
              requestId,
              request,
              response,
              connection));

      ResponseBody body = response.body();
      MediaType contentType = null;
      InputStream responseStream = null;
      if (body != null) {
        contentType = body.contentType();
        responseStream = body.byteStream();
      }

      responseStream = mEventReporter.interpretResponseStream(
          requestId,
          contentType != null ? contentType.toString() : null,
          response.header("Content-Encoding"),
          responseStream,
          new DefaultResponseHandler(mEventReporter, requestId));
      if (responseStream != null) {
        response = response.newBuilder()
            .body(new ForwardingResponseBody(body, responseStream))
            .build();
      }
    }

    return response;
  }

  private static class OkHttpInspectorRequest implements NetworkEventReporter.InspectorRequest {
    private final String mRequestId;
    private final Request mRequest;
    private RequestBodyHelper mRequestBodyHelper;

    public OkHttpInspectorRequest(
        String requestId,
        Request request,
        RequestBodyHelper requestBodyHelper) {
      mRequestId = requestId;
      mRequest = request;
      mRequestBodyHelper = requestBodyHelper;
    }

    @Override
    public String id() {
      return mRequestId;
    }

    @Override
    public String friendlyName() {
      // Hmm, can we do better?  tag() perhaps?
      return null;
    }

    @Nullable
    @Override
    public Integer friendlyNameExtra() {
      return null;
    }

    @Override
    public String url() {
      return mRequest.urlString();
    }

    @Override
    public String method() {
      return mRequest.method();
    }

    @Nullable
    @Override
    public byte[] body() throws IOException {
      RequestBody body = mRequest.body();
      if (body == null) {
        return null;
      }
      OutputStream out = mRequestBodyHelper.createBodySink(firstHeaderValue("Content-Encoding"));
      BufferedSink bufferedSink = Okio.buffer(Okio.sink(out));
      try {
        body.writeTo(bufferedSink);
      } finally {
        bufferedSink.close();
      }
      return mRequestBodyHelper.getDisplayBody();
    }

    @Override
    public int headerCount() {
      return mRequest.headers().size();
    }

    @Override
    public String headerName(int index) {
      return mRequest.headers().name(index);
    }

    @Override
    public String headerValue(int index) {
      return mRequest.headers().value(index);
    }

    @Nullable
    @Override
    public String firstHeaderValue(String name) {
      return mRequest.header(name);
    }
  }

  private static class OkHttpInspectorResponse implements NetworkEventReporter.InspectorResponse {
    private final String mRequestId;
    private final Request mRequest;
    private final Response mResponse;
    private final Connection mConnection;

    public OkHttpInspectorResponse(
        String requestId,
        Request request,
        Response response,
        Connection connection) {
      mRequestId = requestId;
      mRequest = request;
      mResponse = response;
      mConnection = connection;
    }

    @Override
    public String requestId() {
      return mRequestId;
    }

    @Override
    public String url() {
      return mRequest.urlString();
    }

    @Override
    public int statusCode() {
      return mResponse.code();
    }

    @Override
    public String reasonPhrase() {
      return mResponse.message();
    }

    @Override
    public boolean connectionReused() {
      // Not sure...
      return false;
    }

    @Override
    public int connectionId() {
      return mConnection.hashCode();
    }

    @Override
    public boolean fromDiskCache() {
      return mResponse.cacheResponse() != null;
    }

    @Override
    public int headerCount() {
      return mResponse.headers().size();
    }

    @Override
    public String headerName(int index) {
      return mResponse.headers().name(index);
    }

    @Override
    public String headerValue(int index) {
      return mResponse.headers().value(index);
    }

    @Nullable
    @Override
    public String firstHeaderValue(String name) {
      return mResponse.header(name);
    }
  }

  private static class ForwardingResponseBody extends ResponseBody {
    private final ResponseBody mBody;
    private final BufferedSource mInterceptedSource;

    public ForwardingResponseBody(ResponseBody body, InputStream interceptedStream) {
      mBody = body;
      mInterceptedSource = Okio.buffer(Okio.source(interceptedStream));
    }

    @Override
    public MediaType contentType() {
      return mBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
      return mBody.contentLength();
    }

    @Override
    public BufferedSource source() {
      // close on the delegating body will actually close this intercepted source, but it
      // was derived from mBody.byteStream() therefore the close will be forwarded all the
      // way to the original.
      return mInterceptedSource;
    }
  }
}

// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.sample;

import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.okhttp.*;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * Very simple centralized network middleware for illustration purposes.
 */
public class Networker {
  private static Networker sInstance;

  private final OkHttpClient mOkHttpClient;

  public static synchronized Networker get() {
    if (sInstance == null) {
      sInstance = new Networker();
    }
    return sInstance;
  }

  private Networker() {
    mOkHttpClient = new OkHttpClient();
    mOkHttpClient.setDispatcher(new Dispatcher(Executors.newFixedThreadPool(4)));
    mOkHttpClient.networkInterceptors().add(new StethoInterceptor());
  }

  public void submit(HttpRequest request, Callback callback) {
    Call call = mOkHttpClient.newCall(request.okHttpRequest);
    call.enqueue(new ForwardingCallback(callback));
  }

  private static class ForwardingCallback implements com.squareup.okhttp.Callback {
    private final Callback mCallback;

    public ForwardingCallback(Callback callback) {
      mCallback = callback;
    }

    @Override
    public void onFailure(Request request, IOException e) {
      mCallback.onFailure(e);
    }

    @Override
    public void onResponse(Response response) throws IOException {
      mCallback.onResponse(new HttpResponse(response.code(), response.body().bytes()));
    }
  }

  public static class HttpRequest {
    public final String friendlyName;
    public final Request okHttpRequest;

    public static Builder newBuilder() {
      return new Builder();
    }

    HttpRequest(Builder b) {
      friendlyName = b.friendlyName;
      okHttpRequest = b.okHttpRequestBuilder.build();
    }

    public static class Builder {
      private final Request.Builder okHttpRequestBuilder = new Request.Builder();
      private String friendlyName;

      Builder() {
      }

      public Builder friendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
        return this;
      }

      public Builder method(Networker.HttpMethod method, byte[] body) {
        okHttpRequestBuilder.method(
            method.toString(),
            body != null ?
                RequestBody.create(MediaType.parse("application/octet-stream"), body) :
                null);
        return this;
      }

      public Builder url(String url) {
        okHttpRequestBuilder.url(url);
        return this;
      }

      public Builder addHeader(String name, String value) {
        okHttpRequestBuilder.addHeader(name, value);
        return this;
      }

      public HttpRequest build() {
        return new HttpRequest(this);
      }
    }
  }

  public static enum HttpMethod {
    GET, POST
  }

  public static class HttpResponse {
    public final int statusCode;
    public final byte[] body;

    HttpResponse(int statusCode, byte[] body) {
      this.statusCode = statusCode;
      this.body = body;
    }
  }

  public interface Callback {
    public void onResponse(HttpResponse result);
    public void onFailure(IOException e);
  }
}

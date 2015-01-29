// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.sample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Pair;

import com.facebook.stetho.inspector.network.DefaultResponseHandler;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;

/**
 * Very simple centralized network middleware for illustration purposes.
 */
public class Networker {
  private static Networker sInstance;

  private final Executor sExecutor = Executors.newFixedThreadPool(4);
  private final NetworkEventReporter mStethoHook = NetworkEventReporterImpl.get();
  private final AtomicInteger mSequenceNumberGenerator = new AtomicInteger(0);

  private static final int READ_TIMEOUT_MS = 10000;
  private static final int CONNECT_TIMEOUT_MS = 15000;

  public static synchronized Networker get() {
    if (sInstance == null) {
      sInstance = new Networker();
    }
    return sInstance;
  }

  private Networker() {
  }

  public void submit(HttpRequest request, Callback callback) {
    request.uniqueId = String.valueOf(mSequenceNumberGenerator.getAndIncrement());
    sExecutor.execute(new HttpRequestTask(request, callback));
  }

  private class HttpRequestTask implements Runnable {
    private final HttpRequest request;
    private final Callback callback;

    public HttpRequestTask(HttpRequest request, Callback callback) {
      this.request = request;
      this.callback = callback;
    }

    @Override
    public void run() {
      try {
        HttpResponse response = doFetch();
        callback.onResponse(response);
      } catch (IOException e) {
        callback.onFailure(e);
      }
    }

    private HttpResponse doFetch() throws IOException {
      if (mStethoHook.isEnabled()) {
        mStethoHook.requestWillBeSent(
            new SimpleInspectorRequest(request));
      }

      HttpURLConnection conn = openConnectionAndSendRequest();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      InputStream responseStream = conn.getInputStream();

      if (mStethoHook.isEnabled()) {
        responseStream = mStethoHook.interpretResponseStream(
            request.uniqueId,
            conn.getHeaderField("Content-Type"),
            responseStream,
            new DefaultResponseHandler(mStethoHook, request.uniqueId));
      }

      if (responseStream != null) {
        copy(responseStream, out, new byte[1024]);
      }
      return new HttpResponse(conn.getResponseCode(), out.toByteArray());
    }

    private HttpURLConnection openConnectionAndSendRequest() throws IOException {
      try {
        URL url = new URL(request.url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setRequestMethod(request.method.toString());
        if (request.method == HttpMethod.POST) {
          conn.setDoOutput(true);
          conn.getOutputStream().write(request.body);
        }

        conn.connect();
        if (mStethoHook.isEnabled()) {
          // Technically we should add headers here as well.
          int requestSize = request.body != null ? request.body.length : 0;
          mStethoHook.dataSent(
              request.uniqueId,
              requestSize,
              requestSize);
          mStethoHook.responseHeadersReceived(new SimpleInspectorResponse(request, conn));
        }
        return conn;
      } catch (IOException ex) {
        if (mStethoHook.isEnabled()) {
          mStethoHook.httpExchangeFailed(request.uniqueId, ex.toString());
        }
        throw ex;
      }
    }
  }

  private static void copy(InputStream in, OutputStream out, byte[] buf) throws IOException {
    if (in == null) {
      return;
    }
    int n;
    while ((n = in.read(buf)) != -1) {
      out.write(buf, 0, n);
    }
  }


  public static class HttpRequest {
    public final String friendlyName;
    public final HttpMethod method;
    public final String url;
    public final byte[] body;
    public final ArrayList<Pair<String, String>> headers;

    String uniqueId;

    public static Builder newBuilder() {
      return new Builder();
    }

    HttpRequest(Builder b) {
      if (b.method == HttpMethod.POST) {
        if (b.body == null) {
          throw new IllegalArgumentException("POST must have a body");
        }
      } else if (b.method == HttpMethod.GET) {
        if (b.body != null) {
          throw new IllegalArgumentException("GET cannot have a body");
        }
      }
      this.friendlyName = b.friendlyName;
      this.method = b.method;
      this.url = b.url;
      this.body = b.body;
      this.headers = b.headers;
    }

    public static class Builder {
      private String friendlyName;
      private Networker.HttpMethod method;
      private String url;
      private byte[] body = null;
      private ArrayList<Pair<String, String>> headers = new ArrayList<Pair<String, String>>();

      Builder() {
      }

      public Builder friendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
        return this;
      }

      public Builder method(Networker.HttpMethod method) {
        this.method = method;
        return this;
      }

      public Builder url(String url) {
        this.url = url;
        return this;
      }

      public Builder body(byte[] body) {
        this.body = body;
        return this;
      }

      public Builder addHeader(String name, String value) {
        this.headers.add(Pair.create(name, value));
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

  private static class SimpleInspectorRequest
      extends SimpleInspectorHeaders
      implements NetworkEventReporter.InspectorRequest {
    private final HttpRequest request;

    public SimpleInspectorRequest(HttpRequest request) {
      super(request.headers);
      this.request = request;
    }

    @Override
    public String id() {
      return request.uniqueId;
    }

    @Override
    public String friendlyName() {
      return request.friendlyName;
    }

    @Override
    public Integer friendlyNameExtra() {
      return null;
    }

    @Override
    public String url() {
      return request.url;
    }

    @Override
    public String method() {
      return request.method.toString();
    }

    @Override
    public byte[] body() throws IOException {
      return request.body;
    }
  }

  private static class SimpleInspectorResponse
      extends SimpleInspectorHeaders
      implements NetworkEventReporter.InspectorResponse {
    private final HttpRequest request;
    private final int statusCode;
    private final String statusMessage;

    public SimpleInspectorResponse(HttpRequest request, HttpURLConnection conn) throws IOException {
      super(convertHeaders(conn.getHeaderFields()));
      this.request = request;
      statusCode = conn.getResponseCode();
      statusMessage = conn.getResponseMessage();
    }

    private static ArrayList<Pair<String, String>> convertHeaders(Map<String, List<String>> map) {
      ArrayList<Pair<String, String>> array = new ArrayList<Pair<String, String>>();
      for (Map.Entry<String, List<String>> mapEntry : map.entrySet()) {
        for (String mapEntryValue : mapEntry.getValue()) {
          // HttpURLConnection puts a weird null entry in the header map that corresponds to
          // the HTTP response line (for instance, HTTP/1.1 200 OK).  Ignore that weirdness...
          if (mapEntry.getKey() != null) {
            array.add(Pair.create(mapEntry.getKey(), mapEntryValue));
          }
        }
      }
      return array;
    }

    @Override
    public String requestId() {
      return request.uniqueId;
    }

    @Override
    public String url() {
      return request.url;
    }

    @Override
    public int statusCode() {
      return statusCode;
    }

    @Override
    public String reasonPhrase() {
      return statusMessage;
    }

    @Override
    public boolean connectionReused() {
      // No idea...
      return false;
    }

    @Override
    public int connectionId() {
      return request.uniqueId.hashCode();
    }

    @Override
    public boolean fromDiskCache() {
      return false;
    }
  }

  private static class SimpleInspectorHeaders implements NetworkEventReporter.InspectorHeaders {
    private final ArrayList<Pair<String, String>> headers;

    public SimpleInspectorHeaders(ArrayList<Pair<String, String>> headers) {
      this.headers = headers;
    }

    @Override
    public int headerCount() {
      return headers.size();
    }

    @Override
    public String headerName(int index) {
      return headers.get(index).first;
    }

    @Override
    public String headerValue(int index) {
      return headers.get(index).second;
    }

    @Override
    public String firstHeaderValue(String name) {
      int N = headerCount();
      for (int i = 0; i < N; i++) {
        if (name.equals(headerName(i))) {
          return headerValue(i);
        }
      }
      return null;
    }
  }
}

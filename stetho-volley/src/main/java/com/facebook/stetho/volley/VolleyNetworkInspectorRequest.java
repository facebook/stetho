package com.facebook.stetho.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.GsonRequest;
import com.facebook.stetho.inspector.network.NetworkEventReporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

/**
 * Created by briangriffey on 2/19/15.
 */
public class VolleyNetworkInspectorRequest implements NetworkEventReporter.InspectorRequest {

  private final String requestId;
  private final Request request;
  private ArrayList<Map.Entry<String, String>> orderedHeaders;

  public VolleyNetworkInspectorRequest(int requestId, Request request) throws AuthFailureError {
    this.requestId = String.valueOf(requestId);
    this.request = request;
    orderedHeaders = new ArrayList<>();

    if (request != null && request.getHeaders() != null) {
      orderedHeaders.addAll(request.getHeaders().entrySet());
    }
  }

  @Override
  public String id() {
    return requestId;
  }

  @Override
  public String friendlyName() {
    return url();
  }

  @Nullable
  @Override
  public Integer friendlyNameExtra() {
    return null;
  }

  @Override
  public String url() {
    return request.getUrl();
  }

  @Override
  public String method() {
    return getMethodName(request.getMethod());
  }

  @Nullable
  @Override
  public byte[] body() throws IOException {
    try {
      return request.getBody();
    } catch (AuthFailureError e) {
      return null;
    }
  }

  @Override
  public int headerCount() {
    return orderedHeaders.size();
  }

  @Override
  public String headerName(int i) {
    return orderedHeaders.get(i).getKey();
  }

  @Override
  public String headerValue(int i) {
    return orderedHeaders.get(i).getValue();
  }

  @Nullable
  @Override
  public String firstHeaderValue(String s) {
    for (Map.Entry<String, String> entry : orderedHeaders) {
      if (entry.getKey().equals(s)) {
        return entry.getValue();
      }
    }

    return null;
  }

  public String getMethodName(int methodCode) {
    switch (methodCode) {
      case Request.Method.GET:
        return "GET";
      case Request.Method.POST:
        return "POST";
      case Request.Method.DELETE:
        return "DELETE";
      case Request.Method.PUT:
        return "PUT";
      default:
        return "GET";
    }
  }
}

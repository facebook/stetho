package com.facebook.stetho.volley;

import com.android.volley.Request;
import com.facebook.stetho.inspector.network.NetworkEventReporter;

import org.apache.http.HttpResponse;

import javax.annotation.Nullable;

/**
 * Response tracker for Stetho, Encapsulates all of the info from an {@link org.apache.http.HttpResponse}
 * and {@link com.android.volley.Request}
 * Created by briangriffey on 2/19/15.
 */
public class VolleyNetworkInspectorResponse implements NetworkEventReporter.InspectorResponse {


  private final HttpResponse response;
  private final String requestId;
  private final Request request;

  /**
   * Constructor that takes the minimum amount of into required to fill out a response
   *
   * @param request   The request that spawned this response
   * @param response  The actual response object
   * @param requestId a requestid that tracks through the call
   */
  public VolleyNetworkInspectorResponse(Request request, HttpResponse response, String requestId) {
    this.response = response;
    this.requestId = requestId;
    this.request = request;
  }

  @Override
  public int headerCount() {
    return response.getAllHeaders().length;
  }

  @Override
  public String headerName(int i) {
    return response.getAllHeaders()[i].getName();
  }

  @Override
  public String headerValue(int i) {
    return response.getAllHeaders()[i].getValue();
  }

  @Nullable
  @Override
  public String firstHeaderValue(String s) {
    return null;
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public String url() {
    return request.getUrl();
  }

  @Override
  public int statusCode() {
    return response.getStatusLine().getStatusCode();
  }

  @Override
  public String reasonPhrase() {
    return null;
  }

  @Override
  public boolean connectionReused() {
    return false;
  }

  @Override
  public int connectionId() {
    return 0;
  }

  @Override
  public boolean fromDiskCache() {
    return false;
  }
}

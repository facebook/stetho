/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.urlconnection;

import com.facebook.stetho.inspector.network.NetworkEventReporter;

import java.io.IOException;
import java.net.HttpURLConnection;

class URLConnectionInspectorResponse
    extends URLConnectionInspectorHeaders
    implements NetworkEventReporter.InspectorResponse {
  private final String mRequestId;
  private final String mUrl;
  private final int mStatusCode;
  private final String mStatusMessage;

  public URLConnectionInspectorResponse(String requestId, HttpURLConnection conn) throws IOException {
    super(Util.convertHeaders(conn.getHeaderFields()));
    mRequestId = requestId;
    mUrl = conn.getURL().toString();
    mStatusCode = conn.getResponseCode();
    mStatusMessage = conn.getResponseMessage();
  }

  @Override
  public String requestId() {
    return mRequestId;
  }

  @Override
  public String url() {
    return mUrl;
  }

  @Override
  public int statusCode() {
    return mStatusCode;
  }

  @Override
  public String reasonPhrase() {
    return mStatusMessage;
  }

  @Override
  public boolean connectionReused() {
    // No idea...
    return false;
  }

  @Override
  public int connectionId() {
    return mRequestId.hashCode();
  }

  @Override
  public boolean fromDiskCache() {
    return false;
  }
}

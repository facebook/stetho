/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.urlconnection;

import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.RequestBodyHelper;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

class URLConnectionInspectorRequest
    extends URLConnectionInspectorHeaders
    implements NetworkEventReporter.InspectorRequest {
  private final String mRequestId;
  private final String mFriendlyName;
  @Nullable private final SimpleRequestEntity mRequestEntity;
  private final RequestBodyHelper mRequestBodyHelper;
  private final String mUrl;
  private final String mMethod;

  public URLConnectionInspectorRequest(
      String requestId,
      String friendlyName,
      HttpURLConnection configuredRequest,
      @Nullable SimpleRequestEntity requestEntity,
      RequestBodyHelper requestBodyHelper) {
    super(Util.convertHeaders(configuredRequest.getRequestProperties()));
    mRequestId = requestId;
    mFriendlyName = friendlyName;
    mRequestEntity = requestEntity;
    mRequestBodyHelper = requestBodyHelper;
    mUrl = configuredRequest.getURL().toString();
    mMethod = configuredRequest.getRequestMethod();
  }

  @Override
  public String id() {
    return mRequestId;
  }

  @Override
  public String friendlyName() {
    return mFriendlyName;
  }

  @Override
  public Integer friendlyNameExtra() {
    return null;
  }

  @Override
  public String url() {
    return mUrl;
  }

  @Override
  public String method() {
    return mMethod;
  }

  @Nullable
  @Override
  public byte[] body() throws IOException {
    if (mRequestEntity != null) {
      OutputStream out = mRequestBodyHelper.createBodySink(firstHeaderValue("Content-Encoding"));
      try {
        mRequestEntity.writeTo(out);
      } finally {
        out.close();
      }
      return mRequestBodyHelper.getDisplayBody();
    } else {
      return null;
    }
  }
}

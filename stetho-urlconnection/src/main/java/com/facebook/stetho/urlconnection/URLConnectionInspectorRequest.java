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

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

class URLConnectionInspectorRequest
    extends URLConnectionInspectorHeaders
    implements NetworkEventReporter.InspectorRequest {
  private final String mRequestId;
  private final String mFriendlyName;
  @Nullable private final SimpleRequestEntity mRequestEntity;
  private final String mUrl;
  private final String mMethod;

  private boolean mBodyRead;
  @Nullable private byte[] mBody;

  public URLConnectionInspectorRequest(
      String requestId,
      String friendlyName,
      HttpURLConnection configuredRequest,
      @Nullable SimpleRequestEntity requestEntity) {
    super(Util.convertHeaders(configuredRequest.getRequestProperties()));
    mRequestId = requestId;
    mFriendlyName = friendlyName;
    mRequestEntity = requestEntity;
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
      if (!mBodyRead) {
        mBodyRead = true;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mRequestEntity.writeTo(out);
        mBody = out.toByteArray();
      }
      return mBody;
    } else {
      return null;
    }
  }
}

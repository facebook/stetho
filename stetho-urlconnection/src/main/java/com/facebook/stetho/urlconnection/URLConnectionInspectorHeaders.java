/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.urlconnection;

import android.util.Pair;
import com.facebook.stetho.inspector.network.NetworkEventReporter;

import java.util.ArrayList;

class URLConnectionInspectorHeaders implements NetworkEventReporter.InspectorHeaders {
  private final ArrayList<Pair<String, String>> mHeaders;

  public URLConnectionInspectorHeaders(ArrayList<Pair<String, String>> headers) {
    mHeaders = headers;
  }

  @Override
  public int headerCount() {
    return mHeaders.size();
  }

  @Override
  public String headerName(int index) {
    return mHeaders.get(index).first;
  }

  @Override
  public String headerValue(int index) {
    return mHeaders.get(index).second;
  }

  @Override
  public String firstHeaderValue(String name) {
    int N = headerCount();
    for (int i = 0; i < N; i++) {
      if (name.equalsIgnoreCase(headerName(i))) {
        return headerValue(i);
      }
    }
    return null;
  }
}

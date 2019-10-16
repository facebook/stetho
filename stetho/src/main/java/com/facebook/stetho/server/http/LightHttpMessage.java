/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.server.http;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class LightHttpMessage {
  public final ArrayList<String> headerNames = new ArrayList<>();
  public final ArrayList<String> headerValues = new ArrayList<>();

  public void addHeader(String name, String value) {
    headerNames.add(name);
    headerValues.add(value);
  }

  @Nullable
  public String getFirstHeaderValue(String name) {
    for (int i = 0, N = headerNames.size(); i < N; i++) {
      if (name.equals(headerNames.get(i))) {
        return headerValues.get(i);
      }
    }
    return null;
  }

  public void reset() {
    headerNames.clear();
    headerValues.clear();
  }
}

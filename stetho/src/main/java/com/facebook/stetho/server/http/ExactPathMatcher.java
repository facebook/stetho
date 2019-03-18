/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.server.http;

public class ExactPathMatcher implements PathMatcher {
  private final String mPath;

  public ExactPathMatcher(String path) {
    mPath = path;
  }

  @Override
  public boolean match(String path) {
    return mPath.equals(path);
  }
}

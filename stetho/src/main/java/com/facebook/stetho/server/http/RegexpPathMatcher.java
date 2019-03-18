/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.server.http;

import java.util.regex.Pattern;

public class RegexpPathMatcher implements PathMatcher {
  private final Pattern mPattern;

  public RegexpPathMatcher(Pattern pattern) {
    mPattern = pattern;
  }

  @Override
  public boolean match(String path) {
    return mPattern.matcher(path).matches();
  }
}

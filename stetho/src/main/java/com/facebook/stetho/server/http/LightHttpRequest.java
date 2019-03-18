/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.server.http;

import android.net.Uri;

public class LightHttpRequest extends LightHttpMessage {
  public String method;
  public Uri uri;
  public String protocol;

  @Override
  public void reset() {
    super.reset();
    this.method = null;
    this.uri = null;
    this.protocol = null;
  }
}

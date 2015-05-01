/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Application;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.DOMProvider;

public class AndroidDOMProviderFactory implements DOMProvider.Factory {
  private final Application mApplication;

  public AndroidDOMProviderFactory(Application application) {
    mApplication = Util.throwIfNull(application);
  }

  @Override
  public DOMProvider create() {
    return new AndroidDOMProvider(mApplication);
  }
}

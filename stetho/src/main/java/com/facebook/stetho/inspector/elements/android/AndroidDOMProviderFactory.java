// Copyright 2004-present Facebook. All Rights Reserved.

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

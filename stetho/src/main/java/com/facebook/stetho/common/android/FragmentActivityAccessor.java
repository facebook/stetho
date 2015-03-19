// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.app.Activity;

import javax.annotation.Nullable;

public interface FragmentActivityAccessor {
  @Nullable
  public Object getFragmentManager(Activity fragmentActivity);

  @Nullable
  public Object getSupportFragmentManager(Activity fragmentActivity);
}

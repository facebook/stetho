// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.view.View;

import javax.annotation.Nullable;

public interface FragmentAccessor {
  public static final int NO_ID = -1;

  @Nullable
  public Object getFragmentManager(Object fragment);

  public int getId(Object fragment);

  @Nullable
  public String getTag(Object fragment);

  @Nullable
  public View getView(Object fragment);

  @Nullable
  public Object peekChildFragmentManager(Object fragment);
}

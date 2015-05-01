// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.content.res.Resources;
import android.view.View;

import javax.annotation.Nullable;

public interface FragmentAccessor<FRAGMENT, FRAGMENT_MANAGER> {
  public static final int NO_ID = View.NO_ID;

  @Nullable
  public FRAGMENT_MANAGER getFragmentManager(FRAGMENT fragment);

  public Resources getResources(FRAGMENT fragment);

  public int getId(FRAGMENT fragment);

  @Nullable
  public String getTag(FRAGMENT fragment);

  @Nullable
  public View getView(FRAGMENT fragment);

  @Nullable
  public FRAGMENT_MANAGER getChildFragmentManager(FRAGMENT fragment);
}

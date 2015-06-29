/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.content.res.Resources;
import android.view.View;

import javax.annotation.Nullable;

public interface FragmentAccessor<FRAGMENT, FRAGMENT_MANAGER> {
  public static final int NO_ID = 0;

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

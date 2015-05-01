// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.app.Activity;

import javax.annotation.Nullable;

public interface FragmentActivityAccessor<
    FRAGMENT_ACTIVITY extends Activity,
    FRAGMENT_MANAGER> {
  @Nullable
  public FRAGMENT_MANAGER getFragmentManager(FRAGMENT_ACTIVITY activity);
}

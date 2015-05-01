// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import java.util.List;

import javax.annotation.Nullable;

public interface FragmentManagerAccessor<FRAGMENT_MANAGER, FRAGMENT> {
  @Nullable
  public List<FRAGMENT> getAddedFragments(FRAGMENT_MANAGER fragmentManager);
}

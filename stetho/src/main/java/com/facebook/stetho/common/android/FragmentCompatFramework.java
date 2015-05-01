/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;

import javax.annotation.Nullable;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
final class FragmentCompatFramework
    extends FragmentCompat<Fragment, FragmentManager, Activity> {
  private static final FragmentAccessorFrameworkHoneycomb sFragmentAccessor;
  private static final FragmentManagerAccessorViaReflection<FragmentManager, Fragment>
      sFragmentManagerAccessor =
          new FragmentManagerAccessorViaReflection<>(FragmentManager.class);
  private static final FragmentActivityAccessorFramework sFragmentActivityAccessor =
      new FragmentActivityAccessorFramework();

  static {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      sFragmentAccessor = new FragmentAccessorFrameworkJellyBean();
    } else {
      sFragmentAccessor = new FragmentAccessorFrameworkHoneycomb();
    }
  }

  @Override
  public Class<Fragment> getFragmentClass() {
    return Fragment.class;
  }

  @Override
  public Class<Activity> getFragmentActivityClass() {
    return Activity.class;
  }

  @Override
  public FragmentAccessorFrameworkHoneycomb forFragment() {
    return sFragmentAccessor;
  }

  @Override
  public FragmentManagerAccessorViaReflection<FragmentManager, Fragment> forFragmentManager() {
    return sFragmentManagerAccessor;
  }

  @Override
  public FragmentActivityAccessorFramework forFragmentActivity() {
    return sFragmentActivityAccessor;
  }

  private static class FragmentAccessorFrameworkHoneycomb
      implements FragmentAccessor<Fragment, FragmentManager> {
    @Nullable
    @Override
    public FragmentManager getFragmentManager(Fragment fragment) {
      return fragment.getFragmentManager();
    }

    @Override
    public Resources getResources(Fragment fragment) {
      return fragment.getResources();
    }

    @Override
    public int getId(Fragment fragment) {
      return fragment.getId();
    }

    @Nullable
    @Override
    public String getTag(Fragment fragment) {
      return fragment.getTag();
    }

    @Nullable
    @Override
    public View getView(Fragment fragment) {
      return fragment.getView();
    }

    @Nullable
    @Override
    public FragmentManager getChildFragmentManager(Fragment fragment) {
      return null;
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private static class FragmentAccessorFrameworkJellyBean
      extends FragmentAccessorFrameworkHoneycomb {
    @Nullable
    @Override
    public FragmentManager getChildFragmentManager(Fragment fragment) {
      return fragment.getChildFragmentManager();
    }
  }

  private static class FragmentActivityAccessorFramework
      implements FragmentActivityAccessor<Activity, FragmentManager> {
    @Nullable
    @Override
    public FragmentManager getFragmentManager(Activity activity) {
      return activity.getFragmentManager();
    }
  }
}

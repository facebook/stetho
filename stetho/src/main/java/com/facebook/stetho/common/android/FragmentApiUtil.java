// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.app.Activity;
import android.view.View;

import java.util.List;

import javax.annotation.Nullable;

public final class FragmentApiUtil {
  private FragmentApiUtil() {
  }

  @Nullable
  public static Object findFragmentForView(View view) {
    Activity activity = ViewUtil.tryGetActivity(view);
    if (activity == null) {
      return null;
    }

    return findFragmentForViewInActivity(activity, view);
  }

  @Nullable
  private static Object findFragmentForViewInActivity(Activity activity, View view) {
    FragmentActivityAccessor accessor = FragmentApi.tryGetFragmentActivityAccessorFor(activity);
    if (accessor == null) {
      return null;
    }

    Object supportFragmentManager = accessor.getSupportFragmentManager(activity);
    if (supportFragmentManager != null) {
      Object fragment = findFragmentForViewInFragmentManager(supportFragmentManager, view);
      if (fragment != null) {
        return fragment;
      }
    }

    Object fragmentManager = accessor.getFragmentManager(activity);
    if (fragmentManager != null) {
      Object fragment = findFragmentForViewInFragmentManager(fragmentManager, view);
      if (fragment != null) {
        return fragment;
      }
    }

    return null;
  }

  @Nullable
  private static Object findFragmentForViewInFragmentManager(Object fragmentManager, View view) {
    FragmentManagerAccessor accessor = FragmentApi.getFragmentManagerAccessorFor(fragmentManager);
    List<?> fragments = accessor.getAddedFragments(fragmentManager);

    if (fragments != null) {
      for (int i = 0; i < fragments.size(); ++i) {
        Object fragment = fragments.get(i);
        Object result = findFragmentForViewInFragment(fragment, view);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  @Nullable
  private static Object findFragmentForViewInFragment(Object fragment, View view) {
    FragmentAccessor accessor = FragmentApi.getFragmentAccessorFor(fragment);

    if (accessor.getView(fragment) == view) {
      return fragment;
    }

    Object childFragmentManager = accessor.peekChildFragmentManager(fragment);
    if (childFragmentManager != null) {
      return findFragmentForViewInFragmentManager(childFragmentManager, view);
    }

    return null;
  }
}

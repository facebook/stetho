/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.app.Activity;
import android.view.View;

import java.util.List;

import javax.annotation.Nullable;

public final class FragmentCompatUtil {
  private FragmentCompatUtil() {
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
    FragmentCompat supportLib = FragmentCompat.getSupportLibInstance();
    if (supportLib != null) {
      Object fragment = findFragmentForViewInActivity(supportLib, activity, view);
      if (fragment != null) {
        return fragment;
      }
    }

    FragmentCompat framework = FragmentCompat.getFrameworkInstance();
    if (framework != null) {
      Object fragment = findFragmentForViewInActivity(framework, activity, view);
      if (fragment != null) {
        return fragment;
      }
    }

    return null;
  }

  private static Object findFragmentForViewInActivity(
      FragmentCompat compat,
      Activity activity,
      View view) {
    return findFragmentForViewInFragmentManager(
        compat,
        compat.forFragmentActivity().getFragmentManager(activity),
        view);
  }

  @Nullable
  private static Object findFragmentForViewInFragmentManager(
      FragmentCompat compat,
      Object fragmentManager,
      View view) {
    List<?> fragments = compat.forFragmentManager().getAddedFragments(fragmentManager);

    if (fragments != null) {
      for (int i = 0; i < fragments.size(); ++i) {
        Object fragment = fragments.get(i);
        Object result = findFragmentForViewInFragment(compat, fragment, view);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  @Nullable
  private static Object findFragmentForViewInFragment(
      FragmentCompat compat,
      Object fragment,
      View view) {
    FragmentAccessor accessor = compat.forFragment();

    if (accessor.getView(fragment) == view) {
      return fragment;
    }

    Object childFragmentManager = accessor.getChildFragmentManager(fragment);
    if (childFragmentManager != null) {
      return findFragmentForViewInFragmentManager(compat, childFragmentManager, view);
    }

    return null;
  }
}

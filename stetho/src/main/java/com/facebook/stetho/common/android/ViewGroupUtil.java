/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.common.android;

import android.view.View;
import android.view.ViewGroup;

public final class ViewGroupUtil {
  private ViewGroupUtil() {
  }

  public static int findChildIndex(ViewGroup parent, View child) {
    int count = parent.getChildCount();
    for (int i = 0; i < count; ++i) {
      if (parent.getChildAt(i) == child) {
        return i;
      }
    }
    return -1;
  }
}

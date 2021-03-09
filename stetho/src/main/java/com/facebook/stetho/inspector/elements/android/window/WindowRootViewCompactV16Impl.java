/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements.android.window;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

class WindowRootViewCompactV16Impl extends WindowRootViewCompat {
  private Context mContext;

  WindowRootViewCompactV16Impl(Context context) {
    this.mContext = context;
  }

  @NonNull
  @Override
  public List<View> getRootViews() {
    WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    Object wm = getOuter(windowManager);
    return getWindowViews(wm);
  }

  private static Object getOuter(Object innerWM) {
    try {
      Field parentField = innerWM.getClass().getDeclaredField("mWindowManager");
      parentField.setAccessible(true);
      Object outerWM = parentField.get(innerWM);
      parentField.setAccessible(false);
      return outerWM;
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<View> getWindowViews(final Object windowManager) {
    try {
      Class clz = windowManager.getClass();
      Field field = clz.getDeclaredField("mViews");
      field.setAccessible(true);
      return Collections.unmodifiableList(Arrays.asList((View[]) field.get(windowManager)));
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}

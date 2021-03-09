/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements.android.window;

import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

class WindowRootViewCompactV19Impl extends WindowRootViewCompat {

  private List<View> mRootViews;

  WindowRootViewCompactV19Impl() {
    try {
      Class wmClz = Class.forName("android.view.WindowManagerGlobal");
      Method getInstanceMethod = wmClz.getDeclaredMethod("getInstance");
      Object managerGlobal = getInstanceMethod.invoke(wmClz);
      Field mViewsField = wmClz.getDeclaredField("mViews");
      mViewsField.setAccessible(true);
      mRootViews = (List<View>) mViewsField.get(managerGlobal);
      mViewsField.setAccessible(false);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  @NonNull
  @Override
  public List<View> getRootViews() {
    return Collections.unmodifiableList(mRootViews);
  }
}

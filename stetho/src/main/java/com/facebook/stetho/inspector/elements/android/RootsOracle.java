/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.stetho.inspector.elements.android;

import android.os.Build;
import android.os.Looper;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Thread.currentThread;

/**
 * Based on Espresso RootsOracle class:
 * https://android.googlesource.com/platform/frameworks/testing/+/android-support-test/espresso/
 * core/src/main/java/android/support/test/espresso/base/RootsOracle.java
 */
final class RootsOracle {
  private static final String WINDOW_MANAGER_IMPL_CLASS = "android.view.WindowManagerImpl";
  private static final String WINDOW_MANAGER_GLOBAL_CLASS = "android.view.WindowManagerGlobal";
  private static final String VIEWS_FIELD = "mViews";
  private static final String GET_DEFAULT_IMPL = "getDefault";
  private static final String GET_GLOBAL_INSTANCE = "getInstance";

  private boolean initialized;
  private Object windowManager;
  private Field viewsField;

  /**
   * Looks for all Root views
   */
  public List<View> findAllRootViews() {
    if (currentThread() != Looper.getMainLooper().getThread()) {
      throw new UnsupportedOperationException("Expecting main thread, not " + currentThread());
    }
    if (!initialized) {
      initialize();
    }

    if (windowManager == null || viewsField == null) {
      return Collections.emptyList();
    }

    try {
      if (Build.VERSION.SDK_INT < 19) {
        return Arrays.asList((View[]) viewsField.get(windowManager));
      } else {
        //noinspection unchecked
        return new ArrayList<>((List<View>) viewsField.get(windowManager));
      }
    } catch (RuntimeException ignored) {
      return Collections.emptyList();
    } catch (IllegalAccessException ignored) {
      return Collections.emptyList();
    }
  }

  private void initialize() {
    initialized = true;
    String accessClass;
    String instanceMethod;
    if (Build.VERSION.SDK_INT > 16) {
      accessClass = WINDOW_MANAGER_GLOBAL_CLASS;
      instanceMethod = GET_GLOBAL_INSTANCE;
    } else {
      accessClass = WINDOW_MANAGER_IMPL_CLASS;
      instanceMethod = GET_DEFAULT_IMPL;
    }

    try {
      Class<?> clazz = Class.forName(accessClass);
      Method getMethod = clazz.getMethod(instanceMethod);
      windowManager = getMethod.invoke(null);
      viewsField = clazz.getDeclaredField(VIEWS_FIELD);
      viewsField.setAccessible(true);
    } catch (Exception ignored) {
    }
  }
}

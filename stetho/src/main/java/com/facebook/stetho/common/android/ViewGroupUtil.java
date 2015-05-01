/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.stetho.common.ReflectionUtil;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

public final class ViewGroupUtil {
  private static final Field sOnHierarchyChangeListenerField =
      tryGetOnHierarchyChangeListenerField();

  @Nullable
  private static Field tryGetOnHierarchyChangeListenerField() {
    Field field = ReflectionUtil.tryGetDeclaredField(
        ViewGroup.class,
        "mOnHierarchyChangeListener");

    if (field != null) {
      field.setAccessible(true);
    }

    return field;
  }

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

  @Nullable
  public static ViewGroup.OnHierarchyChangeListener tryGetOnHierarchyChangeListenerHack(
      ViewGroup viewGroup) {
    if (sOnHierarchyChangeListenerField == null) {
      return null;
    }

    return (ViewGroup.OnHierarchyChangeListener)ReflectionUtil.getFieldValue(
        sOnHierarchyChangeListenerField,
        viewGroup);
  }
}

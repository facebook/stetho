/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.common;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

public final class ReflectionUtil {
  private ReflectionUtil() {
  }

  @Nullable
  public static Class<?> tryGetClassForName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Nullable
  public static Field tryGetDeclaredField(Class<?> theClass, String fieldName) {
    try {
      return theClass.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      LogUtil.d(
          e,
          "Could not retrieve %s field from %s",
          fieldName,
          theClass);

      return null;
    }
  }

  @Nullable
  public static Object getFieldValue(Field field, Object target) {
    try {
      return field.get(target);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}

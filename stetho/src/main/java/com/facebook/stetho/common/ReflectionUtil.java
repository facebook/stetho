// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

public final class ReflectionUtil {
  private static final Object[] sEmptyArray = new Object[0];

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

  public static Method getMethod(Class<?> theClass, String methodName) {
    try {
      return theClass.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public static Method tryGetMethod(Class<?> theClass, String methodName) {
    try {
      return theClass.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @Nullable
  public static Object invokeMethod(Method method, @Nullable Object target) {
    try {
      return method.invoke(target, sEmptyArray);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
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

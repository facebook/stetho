// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.app.Activity;
import android.view.View;

import com.facebook.stetho.common.ReflectionUtil;
import com.facebook.stetho.common.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;

// TODO: Don't use reflection to implement the accessors. We cannot express an optional dependency
//       on the v4 support library with our current maven plugin for Gradle (need to upgrade to
//       maven-publish?).
//       Having 2 accessor implementations, one for the support API and another for the native API,
//       will yield better performance and much cleaner code.
//       (We will still need to use reflection to access fields, e.g. FragmentManagerImpl.mAdded)

public final class FragmentApi {
  private static final Class<?> sSupportFragmentClass;
  private static final Class<?> sSupportFragmentActivityClass;
  private static final Class<?> sSupportFragmentManagerClass;

  private static final FragmentAccessor sSupportFragmentAccessor;
  private static final FragmentActivityAccessor sSupportFragmentActivityAccessor;
  private static final FragmentManagerAccessor sSupportFragmentManagerAccessor;

  private static final Class<?> sFragmentClass;
  private static final Class<?> sActivityClass;
  private static final Class<?> sFragmentManagerClass;

  private static final FragmentAccessor sFragmentAccessor;
  private static final FragmentActivityAccessor sActivityAccessor;
  private static final FragmentManagerAccessor sFragmentManagerAccessor;

  static {
    sSupportFragmentClass = ReflectionUtil.tryGetClassForName("android.support.v4.app.Fragment");
    sSupportFragmentAccessor = (sSupportFragmentClass != null)
        ? new ReflectionFragmentAccessor(sSupportFragmentClass)
        : null;

    sSupportFragmentActivityClass = ReflectionUtil.tryGetClassForName(
        "android.support.v4.app.FragmentActivity");
    sSupportFragmentActivityAccessor = (sSupportFragmentActivityClass != null)
        ? new ReflectionFragmentActivityAccessor(sSupportFragmentActivityClass)
        : null;

    sSupportFragmentManagerClass = ReflectionUtil.tryGetClassForName(
        "android.support.v4.app.FragmentManagerImpl");
    sSupportFragmentManagerAccessor = (sSupportFragmentManagerClass != null)
        ? new ReflectionFragmentManagerAccessor(sSupportFragmentManagerClass)
        : null;

    sFragmentClass = ReflectionUtil.tryGetClassForName("android.app.Fragment");
    sFragmentAccessor = (sFragmentClass != null)
        ? new ReflectionFragmentAccessor(sFragmentClass)
        : null;

    sActivityClass = ReflectionUtil.tryGetClassForName("android.app.Activity");
    sActivityAccessor = (sActivityClass != null)
        ? new ReflectionFragmentActivityAccessor(sActivityClass)
        : null;

    sFragmentManagerClass = ReflectionUtil.tryGetClassForName("android.app.FragmentManagerImpl");
    sFragmentManagerAccessor = (sFragmentManagerClass != null)
        ? new ReflectionFragmentManagerAccessor(sFragmentManagerClass)
        : null;
  }

  @Nullable
  public static Class<?> tryGetFragmentClass() {
    return sFragmentClass;
  }

  @Nullable
  public static Class<?> tryGetSupportFragmentClass() {
    return sSupportFragmentClass;
  }

  public static FragmentAccessor getFragmentAccessorFor(Object fragment) {
    Util.throwIfNull(fragment);

    if (sSupportFragmentClass != null &&
        sSupportFragmentClass.isAssignableFrom(fragment.getClass())) {
      return sSupportFragmentAccessor;
    }

    if (sFragmentClass != null &&
        sFragmentClass.isAssignableFrom(fragment.getClass())) {
      return sFragmentAccessor;
    }

    throw new IllegalArgumentException();
  }

  @Nullable
  public static FragmentActivityAccessor tryGetFragmentActivityAccessorFor(Object fragmentActivity) {
    Util.throwIfNull(fragmentActivity);

    if (sSupportFragmentActivityClass != null &&
        sSupportFragmentActivityClass.isAssignableFrom(fragmentActivity.getClass())) {
      return sSupportFragmentActivityAccessor;
    }

    if (sActivityClass != null &&
        sActivityClass.isAssignableFrom(fragmentActivity.getClass())) {
      return sActivityAccessor;
    }

    return null;
  }

  public static FragmentManagerAccessor getFragmentManagerAccessorFor(Object fragmentManager) {
    if (sSupportFragmentManagerClass != null &&
        sSupportFragmentManagerClass.isAssignableFrom(fragmentManager.getClass())) {
      return sSupportFragmentManagerAccessor;
    }

    if (sFragmentManagerClass != null &&
        sFragmentManagerClass.isAssignableFrom(fragmentManager.getClass())) {
      return sFragmentManagerAccessor;
    }

    throw new IllegalArgumentException();
  }

  private static final class ReflectionFragmentAccessor implements FragmentAccessor {
    // We access the field instead of calling the getChildFragmentManager() method
    // because the method will instantiate a child FragmentManager if it doesn't exist
    @Nullable
    private final Field mFieldMChildFragmentManager;

    private final Method mMethodGetFragmentManager;
    private final Method mMethodGetId;
    private final Method mMethodGetTag;
    private final Method mMethodGetView;

    public ReflectionFragmentAccessor(Class<?> fragmentClass) {
      Util.throwIfNull(fragmentClass);

      mFieldMChildFragmentManager = ReflectionUtil.tryGetDeclaredField(
          fragmentClass,
          "mChildFragmentManager");
      if (mFieldMChildFragmentManager != null) {
        mFieldMChildFragmentManager.setAccessible(true);
      }

      mMethodGetFragmentManager = ReflectionUtil.getMethod(fragmentClass, "getFragmentManager");
      mMethodGetId = ReflectionUtil.getMethod(fragmentClass, "getId");
      mMethodGetTag = ReflectionUtil.getMethod(fragmentClass, "getTag");
      mMethodGetView = ReflectionUtil.getMethod(fragmentClass, "getView");
    }

    @Override
    public Object getFragmentManager(Object fragment) {
      return ReflectionUtil.invokeMethod(mMethodGetFragmentManager, fragment);
    }

    @Override
    public int getId(Object fragment) {
      return (Integer)ReflectionUtil.invokeMethod(mMethodGetId, fragment);
    }

    @Override
    public String getTag(Object fragment) {
      return (String)ReflectionUtil.invokeMethod(mMethodGetTag, fragment);
    }

    @Override
    public View getView(Object fragment) {
      return (View)ReflectionUtil.invokeMethod(mMethodGetView, fragment);
    }

    @Override
    public Object peekChildFragmentManager(Object fragment) {
      return (mFieldMChildFragmentManager != null)
          ? ReflectionUtil.getFieldValue(mFieldMChildFragmentManager, fragment)
          : null;
    }
  }

  private static final class ReflectionFragmentActivityAccessor
      implements FragmentActivityAccessor {
    @Nullable
    private final Method mMethodGetFragmentManager;

    @Nullable
    private final Method mMethodGetSupportFragmentManager;

    public ReflectionFragmentActivityAccessor(Class<?> fragmentActivityClass) {
      mMethodGetFragmentManager = ReflectionUtil.tryGetMethod(
          fragmentActivityClass,
          "getFragmentManager");

      mMethodGetSupportFragmentManager = ReflectionUtil.tryGetMethod(
          fragmentActivityClass,
          "getSupportFragmentManager");
    }

    @Override
    public Object getFragmentManager(Activity fragmentActivity) {
      return (mMethodGetFragmentManager != null)
          ? ReflectionUtil.invokeMethod(mMethodGetFragmentManager, fragmentActivity)
          : null;
    }

    @Override
    public Object getSupportFragmentManager(Activity fragmentActivity) {
      return (mMethodGetSupportFragmentManager != null)
          ? ReflectionUtil.invokeMethod(mMethodGetSupportFragmentManager, fragmentActivity)
          : null;
    }
  }

  private static class ReflectionFragmentManagerAccessor implements FragmentManagerAccessor {
    private final Field mFieldMAdded;

    public ReflectionFragmentManagerAccessor(Class<?> fragmentManagerClass) {
      Util.throwIfNull(fragmentManagerClass);
      mFieldMAdded = ReflectionUtil.tryGetDeclaredField(fragmentManagerClass, "mAdded");
      if (mFieldMAdded != null) {
        mFieldMAdded.setAccessible(true);
      }
    }

    @Override
    public List<?> getAddedFragments(Object fragmentManager) {
      return (mFieldMAdded != null)
          ? (List<?>)ReflectionUtil.getFieldValue(mFieldMAdded, fragmentManager)
          : null;
    }
  }
}

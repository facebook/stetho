/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Looper;

import com.facebook.stetho.common.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Tracks which {@link Activity} instances have been created and not yet destroyed in creation
 * order for use by Stetho features.
 */
@NotThreadSafe
public final class ActivityTracker {
  private static final ActivityTracker sInstance = new ActivityTracker();

  /**
   * Use {@link WeakReference} here to silence a false positive from LeakCanary:
   *   https://github.com/facebook/stetho/issues/319#issuecomment-285699813
   */
  @GuardedBy("Looper.getMainLooper()")
  private final ArrayList<WeakReference<Activity>> mActivities = new ArrayList<>();
  private final List<WeakReference<Activity>> mActivitiesUnmodifiable =
      Collections.unmodifiableList(mActivities);

  private final List<Listener> mListeners = new CopyOnWriteArrayList<>();

  @Nullable
  private AutomaticTracker mAutomaticTracker;

  public static ActivityTracker get() {
    return sInstance;
  }

  public void registerListener(Listener listener) {
    mListeners.add(listener);
  }

  public void unregisterListener(Listener listener) {
    mListeners.remove(listener);
  }

  /**
   * Start automatic tracking.
   */
  public void beginTracking(Application application) {
    if (mAutomaticTracker == null) {
      AutomaticTracker automaticTracker = new AutomaticTracker(application, this /* tracker */);
      automaticTracker.register();
      mAutomaticTracker = automaticTracker;
    }
  }

  public boolean endTracking() {
    if (mAutomaticTracker != null) {
      mAutomaticTracker.unregister();
      mAutomaticTracker = null;
      return true;
    }
    return false;
  }

  public void add(Activity activity) {
    Util.throwIfNull(activity);
    Util.throwIfNot(Looper.myLooper() == Looper.getMainLooper());
    mActivities.add(new WeakReference<>(activity));
    for (Listener listener : mListeners) {
      listener.onActivityAdded(activity);
    }
  }

  public void remove(Activity activity) {
    Util.throwIfNull(activity);
    Util.throwIfNot(Looper.myLooper() == Looper.getMainLooper());
    if (removeFromWeakList(mActivities, activity)) {
      for (Listener listener : mListeners) {
        listener.onActivityRemoved(activity);
      }
    }
  }

  private static <T> boolean removeFromWeakList(ArrayList<WeakReference<T>> haystack, T needle) {
    for (int i = 0, N = haystack.size(); i < N; i++) {
      T hay = haystack.get(i).get();
      if (hay == needle) {
        haystack.remove(i);
        return true;
      }
    }
    return false;
  }

  public List<WeakReference<Activity>> getActivitiesView() {
    return mActivitiesUnmodifiable;
  }

  @Nullable
  public Activity tryGetTopActivity() {
    if (mActivitiesUnmodifiable.isEmpty()) {
      return null;
    }
    for (int i = mActivitiesUnmodifiable.size() - 1; i >= 0; i--) {
      Activity activity = mActivitiesUnmodifiable.get(i).get();
      if (activity != null) {
        return activity;
      }
    }
    return null;
  }

  public interface Listener {
    public void onActivityAdded(Activity activity);
    public void onActivityRemoved(Activity activity);
  }

  private static class AutomaticTracker {
    private final Application mApplication;
    private final ActivityTracker mTracker;

    public AutomaticTracker(Application application, ActivityTracker tracker) {
      mApplication = application;
      mTracker = tracker;
    }

    public void register() {
      mApplication.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
    }

    public void unregister() {
      mApplication.unregisterActivityLifecycleCallbacks(mLifecycleCallbacks);
    }

    private final Application.ActivityLifecycleCallbacks mLifecycleCallbacks =
        new Application.ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(@Nonnull Activity activity, Bundle savedInstanceState) {
        mTracker.add(activity);
      }

      @Override
      public void onActivityStarted(@Nonnull Activity activity) {

      }

      @Override
      public void onActivityResumed(@Nonnull Activity activity) {

      }

      @Override
      public void onActivityPaused(@Nonnull Activity activity) {

      }

      @Override
      public void onActivityStopped(@Nonnull Activity activity) {

      }

      @Override
      public void onActivitySaveInstanceState(@Nonnull Activity activity, @Nonnull Bundle outState) {

      }

      @Override
      public void onActivityDestroyed(@Nonnull Activity activity) {
        mTracker.remove(activity);
      }
    };
  }
}

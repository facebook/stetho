/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class AutomaticActivityTracker {
  private final Application mApplication;
  private final ArrayList<Activity> mActivities = new ArrayList<>();

  public static synchronized AutomaticActivityTracker register(Application application) {
    AutomaticActivityTracker tracker = new AutomaticActivityTracker(application);
    tracker.register();
    return tracker;
  }

  private AutomaticActivityTracker(Application application) {
    mApplication = application;
  }

  // @VisibleForTesting
  void register() {
    mApplication.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
  }

  public void unregister() {
    mApplication.unregisterActivityLifecycleCallbacks(mLifecycleCallbacks);
  }

  /**
   * Retrieve all created and not yet destroyed activities regardless of their paused/stopped
   * state.  Only activities hosted in the current process are returned and only those
   * which were created after {@link #register(android.app.Application)} was invoked.
   * <p/>
   * The returned list is backed by a mutable list which is modified exclusively on the
   * main thread.
   */
  public List<Activity> getActivities() {
    return mActivities;
  }

  private final Application.ActivityLifecycleCallbacks mLifecycleCallbacks =
      new Application.ActivityLifecycleCallbacks() {
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      mActivities.add(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      mActivities.remove(activity);
    }
  };
}

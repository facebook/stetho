// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.app.Activity;

import com.facebook.stetho.common.LogUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApplicationUtil {
  private ApplicationUtil() {
  }

  public static List<Activity> getAllActivities() {
    try {
      return getAllActivitiesHack();
    } catch (Exception e) {
      LogUtil.w(e, "Cannot retrieve list of Activity instances. UI inspection may not work!");
      return Collections.emptyList();
    }
  }

  // HACK: https://androidreclib.wordpress.com/2014/11/22/getting-the-current-activity/
  // TODO: I'm unsure which version(s) of Android this works on
  private static List<Activity> getAllActivitiesHack()
      throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
    Class activityThreadClass = Class.forName("android.app.ActivityThread");
    Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
    Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
    activitiesField.setAccessible(true);
    Map activities = (Map)activitiesField.get(activityThread);
    List<Activity> activitiesList = new ArrayList<Activity>(activities.size());

    for (Object activityRecord : activities.values()) {
      Class activityRecordClass = activityRecord.getClass();
      Field activityField = activityRecordClass.getDeclaredField("activity");
      activityField.setAccessible(true);
      Activity activity = (Activity)activityField.get(activityRecord);
      activitiesList.add(activity);
    }

    return activitiesList;
  }
}

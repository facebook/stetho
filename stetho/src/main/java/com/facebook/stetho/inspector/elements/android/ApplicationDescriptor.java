// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.common.android.ApplicationUtil;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ApplicationDescriptor extends ChainedDescriptor<Application> {
  private static final String TAG = "ApplicationDescriptor";

  private final Map<Application, ElementContext> mElementToContextMap =
      Collections.synchronizedMap(new IdentityHashMap<Application, ElementContext>());

  private ElementContext getContext(Application element) {
    return mElementToContextMap.get(element);
  }

  @Override
  protected void onHook(Application element) {
    ElementContext context = newElementContext();
    context.hook(element);
    mElementToContextMap.put(element, context);
  }

  @Override
  protected void onUnhook(Application element) {
    ElementContext context = mElementToContextMap.remove(element);
    context.unhook();
  }

  @Override
  protected NodeType onGetNodeType(Application element) {
    return NodeType.DOCUMENT_NODE;
  }

  @Override
  protected int onGetChildCount(Application element) {
    ElementContext context = getContext(element);
    return context.getActivitiesList().size();
  }

  @Override
  protected Object onGetChildAt(Application element, int index) {
    ElementContext context = getContext(element);
    return context.getActivitiesList().get(index);
  }

  private ElementContext newElementContext() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return new ElementContextICS();
    } else {
      LogUtil.w(TAG, "Running on pre-ICS: must manually reload inspector when Activity changes");
      return new ElementContextPreICS();
    }
  }

  private abstract class ElementContext {
    public abstract void hook(Application element);

    public abstract void unhook();

    public abstract List<Activity> getActivitiesList();
  }

  private final class ElementContextPreICS extends ElementContext {
    @Override
    public void hook(Application element) {
    }

    @Override
    public void unhook() {
    }

    @Override
    public List<Activity> getActivitiesList() {
      return ApplicationUtil.getAllActivities();
    }
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private final class ElementContextICS extends ElementContext {
    private Application mElement;
    private Application.ActivityLifecycleCallbacks mCallbacks;
    private List<Activity> mActivities;

    @Override
    public void hook(Application element) {
      mElement = Util.throwIfNull(element);
      mActivities = new ArrayList<Activity>();

      // TODO: tree diffing will remove the need to even worry about installing this callback,
      //       which will then allow ~realtime updates for pre-ICS. for now, pre-ICS will just
      //       have to close and re-open the inspector whenever the activity changes.
      //       (this does assume that the hack below for getAllActivitiesHack() works on pre-ICS)
      mCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
          mActivities.add(0, activity);
          getListener().onChildInserted(mElement, null, activity);
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
          getListener().onChildRemoved(mElement, activity);
        }
      };

      mElement.registerActivityLifecycleCallbacks(mCallbacks);

      mActivities = ApplicationUtil.getAllActivities();
    }

    @Override
    public void unhook() {
      if (mElement != null) {
        if (mCallbacks != null) {
          mElement.unregisterActivityLifecycleCallbacks(mCallbacks);
          mCallbacks = null;
        }

        mElement = null;
      }
    }

    @Override
    public List<Activity> getActivitiesList() {
      return mActivities;
    }
  }
}

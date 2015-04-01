// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewParent;

import javax.annotation.Nullable;

public final class ViewUtil {
  private ViewUtil() {
  }

  @Nullable
  public static Activity tryGetActivity(View view) {
    if (view == null) {
      return null;
    }

    Context context = view.getContext();
    if (context instanceof Activity) {
      return (Activity)context;
    }

    ViewParent parent = view.getParent();
    if (parent instanceof View) {
      View parentView = (View)parent;
      return tryGetActivity(parentView);
    }

    return null;
  }
}

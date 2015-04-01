// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.facebook.stetho.common.Predicate;
import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;

public final class ViewUtil {
  private ViewUtil() {
  }

  @Nullable
  public static View hitTest(View view, float x, float y) {
    return hitTest(view, x, y, null /* viewSelector */);
  }

  // x,y are in view's local coordinate space (relative to its own top/left)
  @Nullable
  public static View hitTest(View view, float x, float y, @Nullable Predicate<View> viewSelector) {
    if (!ViewUtil.pointInView(view, x, y)) {
      return null;
    }

    if (!(view instanceof ViewGroup)) {
      return null;
    }

    final ViewGroup viewGroup = (ViewGroup)view;

    // TODO: get list of Views that are sorted in Z- and draw-order, e.g. buildOrderedChildList()
    for (int i = viewGroup.getChildCount() - 1; i >= 0; --i) {
      final View child = viewGroup.getChildAt(i);

      if (child.getVisibility() != View.VISIBLE) {
        continue;
      }

      if (viewSelector != null && !viewSelector.apply(child)) {
        continue;
      }

      PointF localPoint = new PointF();
      if (ViewUtil.isTransformedPointInView(viewGroup, child, x, y, localPoint)) {
        if (child instanceof ViewGroup) {
          return hitTest(child, localPoint.x, localPoint.y, viewSelector);
        } else {
          return child;
        }
      }
    }

    return viewGroup;
  }

  public static boolean pointInView(View view, float localX, float localY) {
    return localX >= 0 && localX < (view.getRight() - view.getLeft())
        && localY >= 0 && localY < (view.getBottom() - view.getTop());
  }

  public static boolean isTransformedPointInView(
      ViewGroup parent,
      View child,
      float x,
      float y,
      @Nullable PointF outLocalPoint) {
    Util.throwIfNull(parent);
    Util.throwIfNull(child);

    float localX = x + parent.getScrollX() - child.getLeft();
    float localY = y + parent.getScrollY() - child.getTop();

    // TODO: handle transforms
    /*Matrix childMatrix = child.getMatrix();
    if (!childMatrix.isIdentity()) {
      final float[] localXY = new float[2];
      localXY[0] = localX;
      localXY[1] = localY;
      child.getInverseMatrix
    }*/

    final boolean isInView = ViewUtil.pointInView(child, localX, localY);
    if (isInView && outLocalPoint != null) {
      outLocalPoint.set(localX, localY);
    }

    return isInView;
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

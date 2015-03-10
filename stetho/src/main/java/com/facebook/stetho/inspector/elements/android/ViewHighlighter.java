// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.annotation.TargetApi;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

abstract class ViewHighlighter {
  public static ViewHighlighter newInstance() {
    // TODO: find ways to do highlighting on older versions too
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return new OverlayHighlighter();
    } else {
      LogUtil.w("Running on pre-JBMR2: View highlighting is not supported");
      return new NoopHighlighter();
    }
  }

  protected ViewHighlighter() {
  }

  public abstract void clearHighlight();

  public abstract void setHighlightedView(
      View view,
      int contentColor,
      int paddingColor,
      int borderColor,
      int marginColor);

  private static final class NoopHighlighter extends ViewHighlighter {
    @Override
    public void clearHighlight() {
    }

    @Override
    public void setHighlightedView(
        View view,
        int contentColor,
        int paddingColor,
        int borderColor,
        int marginColor) {
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  private static final class OverlayHighlighter extends ViewHighlighter {
    // TODO: use the top-level ViewGroupOverlay instead of ViewOverlay so that we don't end up
    //       causing every single view to allocate a ViewOverlay

    private final Handler mHandler;
    private final ColorDrawable mHighlightDrawable;

    // Only assigned on the UI thread
    private View mHighlightedView;

    private AtomicReference<View> mViewToHighlight = new AtomicReference<View>();
    private AtomicInteger mContentColor = new AtomicInteger();

    private final Runnable mHighlightViewOnUiThreadRunnable = new Runnable() {
      @Override
      public void run() {
        highlightViewOnUiThread();
      }
    };

    public OverlayHighlighter() {
      mHandler = new Handler(Looper.getMainLooper());
      mHighlightDrawable = new ColorDrawable();
    }

    @Override
    public void clearHighlight() {
      setHighlightedViewImpl(null, 0, 0, 0, 0);
    }

    @Override
    public void setHighlightedView(
        View view,
        int contentColor,
        int paddingColor,
        int borderColor,
        int marginColor) {
      setHighlightedViewImpl(
          Util.throwIfNull(view),
          contentColor,
          paddingColor,
          borderColor,
          marginColor);
    }

    private void setHighlightedViewImpl(
        @Nullable View view,
        int contentColor,
        int paddingColor,
        int borderColor,
        int marginColor) {
      mHandler.removeCallbacks(mHighlightViewOnUiThreadRunnable);
      mViewToHighlight.set(view);
      mContentColor.set(contentColor);
      mHandler.postDelayed(mHighlightViewOnUiThreadRunnable, 100);
    }

    private void highlightViewOnUiThread() {
      final View viewToHighlight = mViewToHighlight.getAndSet(null);
      if (viewToHighlight == mHighlightedView) {
        return;
      }

      if (mHighlightedView != null) {
        mHighlightedView.getOverlay().remove(mHighlightDrawable);
      }

      if (viewToHighlight != null) {
        mHighlightDrawable.setColor(mContentColor.get());
        mHighlightDrawable.setBounds(0, 0, viewToHighlight.getWidth(), viewToHighlight.getHeight());
        viewToHighlight.getOverlay().add(mHighlightDrawable);
        mHighlightedView = viewToHighlight;
      }
    }
  }
}

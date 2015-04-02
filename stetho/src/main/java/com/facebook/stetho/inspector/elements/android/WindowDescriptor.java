// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.Descriptor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

final class WindowDescriptor extends ChainedDescriptor<Window> implements HighlightableDescriptor {
  private final Map<Window, ElementContext> mElementToContextMap =
      Collections.synchronizedMap(new IdentityHashMap<Window, ElementContext>());

  @Override
  protected void onHook(Window element) {
    ElementContext context = newElementContext();
    context.hook(element);
    mElementToContextMap.put(element, context);
  }

  @Override
  protected void onUnhook(Window element) {
    ElementContext context = mElementToContextMap.remove(element);
    context.unhook();
  }

  @Override
  protected int onGetChildCount(Window element) {
    View decorView = element.peekDecorView();
    return (decorView != null) ? 1 : 0;
  }

  @Override
  protected Object onGetChildAt(Window element, int index) {
    View decorView = element.peekDecorView();
    if (decorView == null) {
      throw new IndexOutOfBoundsException();
    }

    registerDecorView(decorView);
    return decorView;
  }

  @Override
  public View getViewForHighlighting(Object element) {
    Window window = (Window)element;
    return window.peekDecorView();
  }

  private void registerDecorView(View decorView) {
    if (decorView instanceof ViewGroup) {
      Descriptor descriptor = getHost().getDescriptor(decorView);
      if (descriptor instanceof ViewGroupDescriptor) {
        ((ViewGroupDescriptor)descriptor).registerDecorView((ViewGroup)decorView);
      }
    }
  }

  // TODO: We're probably going to switch to another way of determining structural
  //       changes in the View tree. So this good-faith effort gizmo with chaining
  //       Window.Callback should go away soon enough. Pre-HCMR1 should be supported
  //       just fine then too.

  private ElementContext newElementContext() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      return new ElementContextHCMR1();
    } else {
      LogUtil.w(
          "Running on pre-HCMR1: must manually reload inspector after Window installs DecorView");

      return new ElementContext();
    }
  }

  private class ElementContext {
    public void hook(Window window) {
    }

    public void unhook() {
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private final class ElementContextHCMR1 extends ElementContext implements Window.Callback {
    private Window mWindow;
    private Window.Callback mInnerCallback;
    private View mDecorView;

    @Override
    public void hook(Window window) {
      mWindow = window;
      mInnerCallback = mWindow.getCallback();
      mWindow.setCallback(this);
      mDecorView = mWindow.peekDecorView();
    }

    @Override
    public void unhook() {
      if (mWindow != null) {
        if (mInnerCallback != null) {
          if (mWindow.getCallback() == this) {
            mWindow.setCallback(mInnerCallback);
          }
          mInnerCallback = null;
        }

        mDecorView = null;
        mWindow = null;
      }
    }

    // Window.Callback
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.dispatchKeyEvent(event) : false;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.dispatchKeyShortcutEvent(event) : false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.dispatchTouchEvent(event) : false;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.dispatchTrackballEvent(event) : false;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.dispatchGenericMotionEvent(event) : false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.dispatchPopulateAccessibilityEvent(event) : false;
    }

    @Override
    public View onCreatePanelView(int featureId) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.onCreatePanelView(featureId) : null;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.onCreatePanelMenu(featureId, menu) : false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.onPreparePanel(featureId, view, menu) : false;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.onMenuOpened(featureId, menu) : false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.onMenuItemSelected(featureId, item) : false;
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
      if (mWindow != null && mInnerCallback != null) {
        mInnerCallback.onWindowAttributesChanged(attrs);
      }
    }

    @Override
    public void onContentChanged() {
      if (mWindow == null) {
        return;
      }

      if (mInnerCallback != null) {
        mInnerCallback.onContentChanged();
      }

      if (mDecorView == null) {
        mDecorView = mWindow.peekDecorView();
        if (mDecorView != null) {
          registerDecorView(mDecorView);
          getHost().onChildInserted(mWindow, null, mDecorView);
          // TODO: once we have the decorView, we don't need to worry about further changes (AFAIK).
          //       but since we're going to do something else for this (tree diffing), I don't
          //       think we need to worry about removing our callback for now
        }
      }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
      if (mWindow != null && mInnerCallback != null) {
        mInnerCallback.onWindowFocusChanged(hasFocus);
      }
    }

    @Override
    public void onAttachedToWindow() {
      if (mWindow != null && mInnerCallback != null) {
        mInnerCallback.onAttachedToWindow();
      }
    }

    @Override
    public void onDetachedFromWindow() {
      if (mWindow != null && mInnerCallback != null) {
        mInnerCallback.onDetachedFromWindow();
      }
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
      if (mWindow != null && mInnerCallback != null) {
        mInnerCallback.onPanelClosed(featureId, menu);
      }
    }

    @Override
    public boolean onSearchRequested() {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.onSearchRequested() : false;
    }

    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
      return (mWindow != null && mInnerCallback != null) ? mInnerCallback.onWindowStartingActionMode(callback) : null;
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
      if (mWindow != null && mInnerCallback != null) {
        mInnerCallback.onActionModeStarted(mode);
      }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
      if (mWindow != null && mInnerCallback != null) {
        mInnerCallback.onActionModeFinished(mode);
      }
    }
  }
}

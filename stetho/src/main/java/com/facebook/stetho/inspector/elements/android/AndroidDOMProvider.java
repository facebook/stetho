// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.facebook.stetho.common.Predicate;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.common.android.HandlerUtil;
import com.facebook.stetho.common.android.ViewUtil;
import com.facebook.stetho.inspector.elements.DOMProvider;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.DescriptorMap;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.ObjectDescriptor;

import java.util.ArrayList;
import java.util.List;

final class AndroidDOMProvider implements DOMProvider, AndroidDescriptorHost {
  private static final int INSPECT_OVERLAY_COLOR = 0x40FFFFFF;
  private static final int INSPECT_HOVER_COLOR = 0x404040ff;

  private final Application mApplication;
  private final Handler mHandler;
  private final DescriptorMap mDescriptorMap;
  private final AndroidDOMRoot mDOMRoot;
  private final ViewHighlighter mHighlighter;
  private final InspectModeHandler mInspectModeHandler;
  private Listener mListener;

  public AndroidDOMProvider(Application application) {
    mApplication = Util.throwIfNull(application);
    mHandler = new Handler(Looper.getMainLooper());
    mDOMRoot = new AndroidDOMRoot(application);

    mDescriptorMap = new DescriptorMap()
        .beginInit()
        .register(Activity.class, new ActivityDescriptor())
        .register(AndroidDOMRoot.class, mDOMRoot)
        .register(Application.class, new ApplicationDescriptor());
    FragmentDescriptor.register(mDescriptorMap)
        .register(Object.class, new ObjectDescriptor())
        .register(TextView.class, new TextViewDescriptor())
        .register(View.class, new ViewDescriptor())
        .register(ViewGroup.class, new ViewGroupDescriptor())
        .register(Window.class, new WindowDescriptor())
        .setHost(this)
        .endInit();

    mHighlighter = ViewHighlighter.newInstance();
    mInspectModeHandler = new InspectModeHandler();
  }

  // DOMProvider implementation
  @Override
  public void dispose() {
    mHighlighter.clearHighlight();
    mInspectModeHandler.disable();
  }

  @Override
  public void setListener(Listener listener) {
    mListener = listener;
  }

  @Override
  public boolean postAndWait(Runnable r) {
    return HandlerUtil.postAndWait(mHandler, r);
  }

  @Override
  public Object getRootElement() {
    return mDOMRoot;
  }

  @Override
  public NodeDescriptor getNodeDescriptor(Object element) {
    return getDescriptor(element);
  }

  @Override
  public void highlightElement(Object element, int color) {
    View highlightingView = getHighlightingView(element);
    if (highlightingView == null) {
      mHighlighter.clearHighlight();
    } else {
      mHighlighter.setHighlightedView(highlightingView, color);
    }
  }

  @Override
  public void hideHighlight() {
    mHighlighter.clearHighlight();
  }

  @Override
  public void setInspectModeEnabled(boolean enabled) {
    if (enabled) {
      mInspectModeHandler.enable();
    } else {
      mInspectModeHandler.disable();
    }
  }

  // Descriptor.Host implementation
  @Override
  public Descriptor getDescriptor(Object element) {
    return (element == null) ? null : mDescriptorMap.get(element.getClass());
  }

  @Override
  public void onAttributeModified(Object element, String name, String value) {
    mListener.onAttributeModified(element, name, value);
  }

  @Override
  public void onAttributeRemoved(Object element, String name) {
    mListener.onAttributeRemoved(element, name);
  }

  @Override
  public void onChildInserted(Object parentElement, Object previousElement, Object childElement) {
    mListener.onChildInserted(parentElement, previousElement, childElement);
  }

  @Override
  public void onChildRemoved(Object parentElement, Object childElement) {
    mListener.onChildRemoved(parentElement, childElement);
  }

  // AndroidDescriptorHost implementation
  @Override
  public View getHighlightingView(Object element) {
    if (element == null) {
      return null;
    }

    View highlightingView = null;
    Class<?> theClass = element.getClass();
    Descriptor lastDescriptor = null;
    while (highlightingView == null && theClass != null) {
      Descriptor descriptor = mDescriptorMap.get(theClass);
      if (descriptor == null) {
        return null;
      }

      if (descriptor != lastDescriptor && descriptor instanceof HighlightableDescriptor) {
        highlightingView = ((HighlightableDescriptor)descriptor).getViewForHighlighting(element);
      }

      lastDescriptor = descriptor;
      theClass = theClass.getSuperclass();
    }

    return highlightingView;
  }

  private List<Window> collectWindows() {
    ArrayList<Window> windows = new ArrayList<Window>();

    Descriptor appDescriptor = getDescriptor(mApplication);
    if (appDescriptor == null) {
      return windows;
    }

    int activityCount = appDescriptor.getChildCount(mApplication);
    for (int ai = 0; ai < activityCount; ++ai) {
      final Activity activity = (Activity)appDescriptor.getChildAt(mApplication, ai);
      Descriptor activityDescriptor = getDescriptor(activity);
      if (activityDescriptor == null) {
        continue;
      }

      int windowCount = activityDescriptor.getChildCount(activity);
      for (int wi = 0; wi < windowCount; ++wi) {
        final Window window = (Window)activityDescriptor.getChildAt(activity, wi);
        windows.add(window);
      }
    }

    return windows;
  }

  private final class InspectModeHandler {
    private final Runnable mEnableOnUiThreadRunnable = new Runnable() {
      @Override
      public void run() {
        enableOnUiThread();
      }
    };

    private final Runnable mDisableOnUiThreadRunnable = new Runnable() {
      @Override
      public void run() {
        disableOnUiThread();
      }
    };

    private final Predicate<View> mViewSelector = new Predicate<View>() {
      @Override
      public boolean apply(View view) {
        return !(view instanceof DOMHiddenView);
      }
    };

    private List<View> mOverlays;

    public void enable() {
      mHandler.post(mEnableOnUiThreadRunnable);
    }

    private void enableOnUiThread() {
      if (mOverlays != null) {
        disableOnUiThread();
      }

      mOverlays = new ArrayList<View>();

      List<Window> windows = collectWindows();
      for (int i = 0; i < windows.size(); ++i) {
        final Window window = windows.get(i);
        if (window.peekDecorView() instanceof ViewGroup) {
          final ViewGroup decorView = (ViewGroup)window.peekDecorView();

          OverlayView overlayView = new OverlayView(mApplication);

          WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
          layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
          layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;

          decorView.addView(overlayView, layoutParams);
          decorView.bringChildToFront(overlayView);

          mOverlays.add(overlayView);
        }
      }
    }

    public void disable() {
      mHandler.post(mDisableOnUiThreadRunnable);
    }

    private void disableOnUiThread() {
      if (mOverlays == null) {
        return;
      }

      for (int i = 0; i < mOverlays.size(); ++i) {
        final View overlayView = mOverlays.get(i);
        ViewGroup decorViewGroup = (ViewGroup)overlayView.getParent();
        decorViewGroup.removeView(overlayView);
      }

      mOverlays = null;
    }

    private final class OverlayView extends DOMHiddenView {
      public OverlayView(Context context) {
        super(context);
      }

      @Override
      protected void onDraw(Canvas canvas) {
        canvas.drawColor(INSPECT_OVERLAY_COLOR);
        super.onDraw(canvas);
      }

      @Override
      public boolean onTouchEvent(MotionEvent event) {
        if (getParent() instanceof View) {
          final View parent = (View)getParent();
          View view = ViewUtil.hitTestTouch(parent, event.getX(), event.getY(), mViewSelector);

          if (event.getAction() != MotionEvent.ACTION_CANCEL) {
            if (view != null) {
              mHighlighter.setHighlightedView(view, INSPECT_HOVER_COLOR);

              if (event.getAction() == MotionEvent.ACTION_UP) {
                mListener.onInspectRequested(view);
              }
            }
          }
        }

        return true;
      }
    }
  }
}

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.Predicate;
import com.facebook.stetho.common.ThreadBound;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.DocumentProvider;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.DescriptorProvider;
import com.facebook.stetho.inspector.elements.DescriptorMap;
import com.facebook.stetho.inspector.elements.DocumentProviderListener;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.ObjectDescriptor;
import com.facebook.stetho.inspector.helper.ThreadBoundProxy;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

final class AndroidDocumentProvider extends ThreadBoundProxy
    implements DocumentProvider, AndroidDescriptorHost {
  private static final int INSPECT_OVERLAY_COLOR = 0x40FFFFFF;
  private static final int INSPECT_HOVER_COLOR = 0x404040ff;

  private final Rect mHighlightingBoundsRect = new Rect();
  private final Rect mHitRect = new Rect();

  private final Application mApplication;
  private final DescriptorMap mDescriptorMap;
  private final AndroidDocumentRoot mDocumentRoot;
  private final ViewHighlighter mHighlighter;
  private final InspectModeHandler mInspectModeHandler;
  private @Nullable DocumentProviderListener mListener;

  // We don't yet have an an implementation for reliably detecting fine-grained changes in the
  // View tree. So, for now at least, we have a timer that runs every so often and just reports
  // that we changed. Our listener will then read the entire Document from us and transmit the
  // changes to Chrome. Detecting, reporting, and traversing fine-grained changes is a future work
  // item (see Issue #210).
  private static final long REPORT_CHANGED_INTERVAL_MS = 1000;
  private boolean mIsReportChangesTimerPosted = false;
  private final Runnable mReportChangesTimer = new Runnable() {
    @Override
    public void run() {
      mIsReportChangesTimerPosted = false;

      if (mListener != null) {
        mListener.onPossiblyChanged();
        mIsReportChangesTimerPosted = true;
        postDelayed(this, REPORT_CHANGED_INTERVAL_MS);
      }
    }
  };

  public AndroidDocumentProvider(
      Application application,
      List<DescriptorProvider> descriptorProviders,
      ThreadBound enforcer) {
    super(enforcer);

    mApplication = Util.throwIfNull(application);
    mDocumentRoot = new AndroidDocumentRoot(application);

    mDescriptorMap = new DescriptorMap()
        .beginInit()
        .registerDescriptor(Activity.class, new ActivityDescriptor())
        .registerDescriptor(AndroidDocumentRoot.class, mDocumentRoot)
        .registerDescriptor(Application.class, new ApplicationDescriptor())
        .registerDescriptor(Dialog.class, new DialogDescriptor())
        .registerDescriptor(Object.class, new ObjectDescriptor())
        .registerDescriptor(TextView.class, new TextViewDescriptor())
        .registerDescriptor(View.class, new ViewDescriptor())
        .registerDescriptor(ViewGroup.class, new ViewGroupDescriptor())
        .registerDescriptor(Window.class, new WindowDescriptor());

    DialogFragmentDescriptor.register(mDescriptorMap);
    FragmentDescriptor.register(mDescriptorMap);

    for (int i = 0, size = descriptorProviders.size(); i < size; ++i) {
      final DescriptorProvider descriptorProvider = descriptorProviders.get(i);
      descriptorProvider.registerDescriptor(mDescriptorMap);
    }

    mDescriptorMap.setHost(this).endInit();

    mHighlighter = ViewHighlighter.newInstance();
    mInspectModeHandler = new InspectModeHandler();
  }

  @Override
  public void dispose() {
    verifyThreadAccess();

    mHighlighter.clearHighlight();
    mInspectModeHandler.disable();
    removeCallbacks(mReportChangesTimer);
    mIsReportChangesTimerPosted = false;
    mListener = null;
  }

  @Override
  public void setListener(DocumentProviderListener listener) {
    verifyThreadAccess();

    mListener = listener;
    if (mListener == null && mIsReportChangesTimerPosted) {
      mIsReportChangesTimerPosted = false;
      removeCallbacks(mReportChangesTimer);
    } else if (mListener != null && !mIsReportChangesTimerPosted) {
      mIsReportChangesTimerPosted = true;
      postDelayed(mReportChangesTimer, REPORT_CHANGED_INTERVAL_MS);
    }
  }

  @Override
  public Object getRootElement() {
    verifyThreadAccess();
    return mDocumentRoot;
  }

  @Override
  public NodeDescriptor getNodeDescriptor(Object element) {
    verifyThreadAccess();
    return getDescriptor(element);
  }

  @Override
  public void highlightElement(Object element, int color) {
    verifyThreadAccess();

    final HighlightableDescriptor descriptor = getHighlightableDescriptor(element);
    if (descriptor == null) {
      mHighlighter.clearHighlight();
      return;
    }

    mHighlightingBoundsRect.setEmpty();
    final View highlightingView =
        descriptor.getViewAndBoundsForHighlighting(element, mHighlightingBoundsRect);
    if (highlightingView == null) {
      mHighlighter.clearHighlight();
      return;
    }

    mHighlighter.setHighlightedView(
        highlightingView,
        mHighlightingBoundsRect,
        color);
  }

  @Override
  public void hideHighlight() {
    verifyThreadAccess();

    mHighlighter.clearHighlight();
  }

  @Override
  public void setInspectModeEnabled(boolean enabled) {
    verifyThreadAccess();

    if (enabled) {
      mInspectModeHandler.enable();
    } else {
      mInspectModeHandler.disable();
    }
  }

  @Override
  public void setAttributesAsText(Object element, String text) {
    verifyThreadAccess();

    Descriptor descriptor = mDescriptorMap.get(element.getClass());
    if (descriptor != null) {
      descriptor.setAttributesAsText(element, text);
    }
  }

  // Descriptor.Host implementation
  @Override
  public Descriptor getDescriptor(Object element) {
    return (element == null) ? null : mDescriptorMap.get(element.getClass());
  }

  @Override
  public void onAttributeModified(Object element, String name, String value) {
    if (mListener != null) {
      mListener.onAttributeModified(element, name, value);
    }
  }

  @Override
  public void onAttributeRemoved(Object element, String name) {
    if (mListener != null) {
      mListener.onAttributeRemoved(element, name);
    }
  }

  // AndroidDescriptorHost implementation
  @Override
  @Nullable
  public HighlightableDescriptor getHighlightableDescriptor(@Nullable Object element) {
    if (element == null) {
      return null;
    }

    HighlightableDescriptor highlightableDescriptor = null;
    Class<?> theClass = element.getClass();
    Descriptor lastDescriptor = null;
    while (highlightableDescriptor == null && theClass != null) {
      Descriptor descriptor = mDescriptorMap.get(theClass);
      if (descriptor == null) {
        return null;
      }

      if (descriptor != lastDescriptor && descriptor instanceof HighlightableDescriptor) {
        highlightableDescriptor = ((HighlightableDescriptor) descriptor);
      }

      lastDescriptor = descriptor;
      theClass = theClass.getSuperclass();
    }

    return highlightableDescriptor;
  }

  private void getWindows(final Accumulator<Window> accumulator) {
    Descriptor appDescriptor = getDescriptor(mApplication);
    if (appDescriptor != null) {
      Accumulator<Object> elementAccumulator = new Accumulator<Object>() {
        @Override
        public void store(Object element) {
          if (element instanceof Window) {
            // Store the Window and do not recurse into its children.
            accumulator.store((Window) element);
          } else {
            // Recursively scan this element's children in search of more Windows.
            Descriptor elementDescriptor = getDescriptor(element);
            if (elementDescriptor != null) {
              elementDescriptor.getChildren(element, this);
            }
          }
        }
      };

      appDescriptor.getChildren(mApplication, elementAccumulator);
    }
  }

  private final class InspectModeHandler {
    private final Predicate<View> mViewSelector = new Predicate<View>() {
      @Override
      public boolean apply(View view) {
        return !(view instanceof DocumentHiddenView);
      }
    };

    private List<View> mOverlays;

    public void enable() {
      verifyThreadAccess();

      if (mOverlays != null) {
        disable();
      }

      mOverlays = new ArrayList<>();

      getWindows(new Accumulator<Window>() {
        @Override
        public void store(Window object) {
          if (object.peekDecorView() instanceof ViewGroup) {
            final ViewGroup decorView = (ViewGroup) object.peekDecorView();

            OverlayView overlayView = new OverlayView(mApplication);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;

            decorView.addView(overlayView, layoutParams);
            decorView.bringChildToFront(overlayView);

            mOverlays.add(overlayView);
          }
        }
      });
    }

    public void disable() {
      verifyThreadAccess();

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

    private final class OverlayView extends DocumentHiddenView {
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
        int x = (int) event.getX();
        int y = (int) event.getY();
        Object elementToHighlight = getParent();
        while (true) {
          final HighlightableDescriptor descriptor =
              getHighlightableDescriptor(elementToHighlight);

          if (descriptor == null) {
            break;
          }

          mHitRect.setEmpty();
          final Object element =
              descriptor.getElementToHighlightAtPosition(elementToHighlight, x, y, mHitRect);

          x -= mHitRect.left;
          y -= mHitRect.top;

          if (element == elementToHighlight) {
            break;
          }

          elementToHighlight = element;
        }

        if (elementToHighlight != null) {
          final HighlightableDescriptor descriptor =
              getHighlightableDescriptor(elementToHighlight);

          if (descriptor != null) {
            final View viewToHighlight =
                descriptor.getViewAndBoundsForHighlighting(
                    elementToHighlight,
                    mHighlightingBoundsRect);

            if (event.getAction() != MotionEvent.ACTION_CANCEL) {
              if (viewToHighlight != null) {
                mHighlighter.setHighlightedView(
                    viewToHighlight,
                    mHighlightingBoundsRect,
                    INSPECT_HOVER_COLOR);

                if (event.getAction() == MotionEvent.ACTION_UP) {
                  if (mListener != null) {
                    mListener.onInspectRequested(elementToHighlight);
                  }
                }
              }
            }
          }
        }

        return true;
      }
    }
  }
}

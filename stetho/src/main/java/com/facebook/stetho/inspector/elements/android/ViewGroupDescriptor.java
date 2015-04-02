// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.common.android.FragmentApiUtil;
import com.facebook.stetho.common.android.ViewGroupUtil;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

final class ViewGroupDescriptor extends ChainedDescriptor<ViewGroup> {
  private final Map<ViewGroup, ElementContext> mElementToContextMap =
      Collections.synchronizedMap(new IdentityHashMap<ViewGroup, ElementContext>());

  public ViewGroupDescriptor() {
  }

  private ElementContext getOrCreateContext(ViewGroup element) {
    ElementContext context = mElementToContextMap.get(element);
    if (context == null) {
      context = new ElementContext();
      mElementToContextMap.put(element, context);
    }
    return context;
  }

  final void registerDecorView(ViewGroup decorView) {
    ElementContext context = getOrCreateContext(decorView);
    context.markDecorView();
  }

  @Override
  protected void onHook(ViewGroup element) {
    ElementContext context = getOrCreateContext(element);
    context.hook(element);
  }

  @Override
  protected void onUnhook(ViewGroup element) {
    ElementContext context = mElementToContextMap.remove(element);
    context.unhook();
  }

  @Override
  protected int onGetChildCount(ViewGroup element) {
    ElementContext context = mElementToContextMap.get(element);
    return context.getChildCount();
  }

  @Override
  protected Object onGetChildAt(ViewGroup element, int index) {
    ElementContext context = mElementToContextMap.get(element);
    return context.getChildAt(index);
  }

  private final class ElementContext implements ViewGroup.OnHierarchyChangeListener {
    // This is a cache that maps from a View to the Fragment that contains it. If the
    // View isn't contained by a Fragment, then this maps the View to itself.
    // For Views contained by Fragments, we emit the Fragment instead, and then let
    // the Fragment's descriptor emit the View as its sole child. This allows us to
    // see Fragments in the inspector as part of the UI tree.
    private final Map<View, Object> mViewToElementMap =
        Collections.synchronizedMap(new IdentityHashMap<View, Object>());

    private ViewGroup mElement;
    private ViewGroup.OnHierarchyChangeListener mInnerListener;
    private boolean mIsDecorView;

    public void hook(ViewGroup element) {
      if (mInnerListener != null) {
        throw new IllegalStateException();
      }

      mElement = Util.throwIfNull(element);
      mInnerListener = ViewGroupUtil.tryGetOnHierarchyChangeListenerHack(mElement);
      mElement.setOnHierarchyChangeListener(this);
    }

    public void unhook() {
      if (mElement != null) {
        ViewGroup.OnHierarchyChangeListener currentListener =
            ViewGroupUtil.tryGetOnHierarchyChangeListenerHack(mElement);

        if (currentListener == this) {
          mElement.setOnHierarchyChangeListener(mInnerListener);
        } else {
          mElement.setOnHierarchyChangeListener(null);
        }

        mInnerListener = null;
        mElement = null;

        mViewToElementMap.clear();
      }
    }

    public void markDecorView() {
      mIsDecorView = true;
    }

    public int getChildCount() {
      if (mIsDecorView) {
        return getDecorViewChildCount();
      } else {
        return mElement.getChildCount();
      }
    }

    private boolean isChildVisible(View child) {
      return !mIsDecorView || !(child instanceof DOMHiddenView);
    }

    private int getDecorViewChildCount() {
      final int realCount = mElement.getChildCount();
      int virtualCount = 0;
      for (int i = 0; i < realCount; ++i) {
        if (isChildVisible(mElement.getChildAt(i))) {
          ++virtualCount;
        }
      }
      return virtualCount;
    }

    public Object getChildAt(int index) {
      if (index < 0 || index >= mElement.getChildCount()) {
        throw new IndexOutOfBoundsException();
      }

      if (mIsDecorView) {
        return getDecorViewChildAt(index);
      } else {
        View view = mElement.getChildAt(index);
        return getElementForView(view);
      }
    }

    private Object getDecorViewChildAt(int index) {
      final int realCount = mElement.getChildCount();
      int virtualIndex = 0;
      for (int i = 0; i < realCount; ++i) {
        View child = mElement.getChildAt(i);
        if (isChildVisible(child)) {
          if (virtualIndex == index) {
            return getElementForView(child);
          }
          ++virtualIndex;
        }
      }

      throw new IndexOutOfBoundsException();
    }

    private Object getElementForView(View view) {
      if (view == null) {
        return null;
      }

      Object element = mViewToElementMap.get(view);
      if (element != null) {
        return element;
      }

      Object fragment = FragmentApiUtil.findFragmentForView(view);
      if (fragment != null) {
        mViewToElementMap.put(view, fragment);
        return fragment;
      } else {
        mViewToElementMap.put(view, view);
        return view;
      }
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
      if (mElement == null) {
        return;
      }

      if (mInnerListener != null) {
        mInnerListener.onChildViewAdded(parent, child);
      }

      if (!isChildVisible(child)) {
        return;
      }

      if (parent instanceof ViewGroup) {
        final ViewGroup parentGroup = (ViewGroup)parent;

        int childIndex = ViewGroupUtil.findChildIndex(parentGroup, child);

        Object previousElement = null;
        for (int i = childIndex - 1; i >= 0; --i) {
          View previousChild = parentGroup.getChildAt(i);
          if (isChildVisible(previousChild)) {
            previousElement = getElementForView(previousChild);
            break;
          }
        }

        Object childElement = getElementForView(child);

        getHost().onChildInserted(parent, previousElement, childElement);
      }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
      if (mElement == null) {
        return;
      }

      if (mInnerListener != null) {
        mInnerListener.onChildViewRemoved(parent, child);
      }

      if (!isChildVisible(child)) {
        return;
      }

      Object childElement = getElementForView(child);
      getHost().onChildRemoved(parent, childElement);

      mViewToElementMap.remove(child);
    }
  }
}

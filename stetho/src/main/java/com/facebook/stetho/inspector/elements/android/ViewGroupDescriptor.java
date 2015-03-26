// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.common.android.FragmentApiUtil;
import com.facebook.stetho.common.android.ViewGroupUtil;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

final class ViewGroupDescriptor extends ChainedDescriptor<ViewGroup> {
  private final Map<ViewGroup, ElementContext> mElementToContextMap =
      Collections.synchronizedMap(new HashMap<ViewGroup, ElementContext>());

  public ViewGroupDescriptor() {
  }

  @Override
  protected void onHook(ViewGroup element) {
    ElementContext context = new ElementContext();
    context.hook(element);
    mElementToContextMap.put(element, context);
  }

  @Override
  protected void onUnhook(ViewGroup element) {
    ElementContext context = mElementToContextMap.remove(element);
    context.unhook();
  }

  @Override
  protected int onGetChildCount(ViewGroup element) {
    return element.getChildCount();
  }

  @Override
  protected Object onGetChildAt(ViewGroup element, int index) {
    ElementContext context = mElementToContextMap.get(element);
    return context.getChildAt(element, index);
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

    public void hook(ViewGroup element) {
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

    public Object getChildAt(ViewGroup element, int index) {
      if (index < 0 || index >= element.getChildCount()) {
        throw new IndexOutOfBoundsException();
      }

      View view = element.getChildAt(index);
      return getElementForView(view);
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

      if (parent instanceof ViewGroup) {
        final ViewGroup parentGroup = (ViewGroup)parent;

        int childIndex = ViewGroupUtil.findChildIndex(parentGroup, child);
        View previousChild = (childIndex == 0) ? null : parentGroup.getChildAt(childIndex - 1);

        Object childElement = getElementForView(child);
        Object previousElement = getElementForView(previousChild);

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

      Object childElement = getElementForView(child);
      getHost().onChildRemoved(parent, childElement);

      mViewToElementMap.remove(child);
    }
  }
}

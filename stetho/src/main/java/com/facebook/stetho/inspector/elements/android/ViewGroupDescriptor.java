// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class ViewGroupDescriptor extends ChainedDescriptor<ViewGroup> {
  // TODO: We're probably going to switch to another way of determining structural
  //       changes in the View tree. So this gizmo with chaining OnHierarchyChangeListener
  //       via reflection should go away soon.

  private static final Field sOnHierarchyChangeListenerField;
  static {
    Field field;
    try {
      field = ViewGroup.class.getDeclaredField("mOnHierarchyChangeListener");
    } catch (NoSuchFieldException ex) {
      field = null;
    }

    sOnHierarchyChangeListenerField = field;

    if (sOnHierarchyChangeListenerField != null) {
      sOnHierarchyChangeListenerField.setAccessible(true);
    }
  }

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
    if (index < 0 || index >= element.getChildCount()) {
      throw new IndexOutOfBoundsException();
    }

    return element.getChildAt(index);
  }

  private static ViewGroup.OnHierarchyChangeListener getOnHierarchyChangeListenerHack(
      ViewGroup viewGroup) throws NoSuchFieldException {
    if (sOnHierarchyChangeListenerField == null) {
      throw new NoSuchFieldException();
    }

    try {
      return (ViewGroup.OnHierarchyChangeListener) sOnHierarchyChangeListenerField.get(viewGroup);
    } catch (IllegalAccessException ex) {
      // should not happen since we called setAccessible(true)
      throw new IllegalAccessError(ex.getMessage());
    }
  }

  private static int findChildIndex(ViewGroup parent, View child) {
    int count = parent.getChildCount();
    for (int i = 0; i < count; ++i) {
      if (parent.getChildAt(i) == child) {
        return i;
      }
    }
    return -1;
  }

  private final class ElementContext implements ViewGroup.OnHierarchyChangeListener {

    private ViewGroup mElement;
    private ViewGroup.OnHierarchyChangeListener mInnerListener;

    public void hook(ViewGroup element) {
      mElement = Util.throwIfNull(element);

      ViewGroup.OnHierarchyChangeListener innerListener;
      try {
        innerListener = getOnHierarchyChangeListenerHack(mElement);
      } catch (NoSuchFieldException ex) {
        innerListener = null;
      }
      mInnerListener = innerListener;

      mElement.setOnHierarchyChangeListener(this);
    }

    public void unhook() {
      if (mElement != null) {
        ViewGroup.OnHierarchyChangeListener currentListener;
        try {
          currentListener = getOnHierarchyChangeListenerHack(mElement);
        } catch (NoSuchFieldException ex) {
          currentListener = null;
        }

        if (currentListener == this) {
          mElement.setOnHierarchyChangeListener(mInnerListener);
        } else {
          mElement.setOnHierarchyChangeListener(null);
        }

        mInnerListener = null;
        mElement = null;
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
        int index = findChildIndex(parentGroup, child);
        View previousChild = (index == 0) ? null : parentGroup.getChildAt(index - 1);
        getListener().onChildInserted(parent, previousChild, child);
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

      getListener().onChildRemoved(parent, child);
    }
  }
}

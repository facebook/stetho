/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.common.android.FragmentCompatUtil;
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
  protected void onGetChildren(ViewGroup element, Accumulator<Object> children) {
    ElementContext context = mElementToContextMap.get(element);
    context.getChildren(children);
  }

  private final class ElementContext {
    // This is a cache that maps from a View to the Fragment that contains it. If the
    // View isn't contained by a Fragment, then this maps the View to itself.
    // For Views contained by Fragments, we emit the Fragment instead, and then let
    // the Fragment's descriptor emit the View as its sole child. This allows us to
    // see Fragments in the inspector as part of the UI tree.
    private final Map<View, Object> mViewToElementMap =
        Collections.synchronizedMap(new IdentityHashMap<View, Object>());

    private ViewGroup mElement;
    private boolean mIsDecorView;

    public void hook(ViewGroup element) {
      mElement = Util.throwIfNull(element);
    }

    public void unhook() {
      if (mElement != null) {
        mElement = null;
        mViewToElementMap.clear();
      }
    }

    public void markDecorView() {
      mIsDecorView = true;
    }

    public void getChildren(Accumulator<Object> children) {
      for (int i = 0, N = mElement.getChildCount(); i < N; ++i) {
        final View child = mElement.getChildAt(i);
        if (isChildVisible(child)) {
          final Object element = getElementForView(child);
          children.store(element);
        }
      }
    }

    private boolean isChildVisible(View child) {
      return !mIsDecorView || !(child instanceof DOMHiddenView);
    }

    private Object getElementForView(View view) {
      if (view == null) {
        return null;
      }

      Object element = mViewToElementMap.get(view);
      if (element != null) {
        return element;
      }

      Object fragment = FragmentCompatUtil.findFragmentForView(view);
      if (fragment != null) {
        mViewToElementMap.put(view, fragment);
        return fragment;
      } else {
        mViewToElementMap.put(view, view);
        return view;
      }
    }
  }
}

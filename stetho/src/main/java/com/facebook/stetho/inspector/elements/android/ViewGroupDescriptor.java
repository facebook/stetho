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
import com.facebook.stetho.common.android.FragmentCompatUtil;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class ViewGroupDescriptor extends AbstractChainedDescriptor<ViewGroup> {
  /**
   * This is a cache that maps from a View to the Fragment that contains it. If the View isn't
   * contained by a Fragment, then this maps the View to itself. For Views contained by Fragments,
   * we emit the Fragment instead, and then let the Fragment's descriptor emit the View as its sole
   * child. This allows us to see Fragments in the inspector as part of the UI tree.
   */
  private final Map<View, Object> mViewToElementMap =
      Collections.synchronizedMap(new WeakHashMap<View, Object>());

  public ViewGroupDescriptor() {
  }

  @Override
  protected void onGetChildren(ViewGroup element, Accumulator<Object> children) {
    for (int i = 0, N = element.getChildCount(); i < N; ++i) {
      final View childView = element.getChildAt(i);
      if (isChildVisible(childView)) {
        final Object childElement = getElementForView(element, childView);
        children.store(childElement);
      }
    }
  }

  private boolean isChildVisible(View child) {
    return !(child instanceof DOMHiddenView);
  }

  private Object getElementForView(ViewGroup parentView, View childView) {
    Object element = mViewToElementMap.get(childView);
    if (element != null) {
      // The parent of a View may have changed since we stashed it into the cache.
      // If that's the case then we can't use the cache's answer.
      if (childView.getParent() == parentView) {
        return element;
      }
      mViewToElementMap.remove(childView);
    }

    /**
     * Note that we do NOT emit DialogFragments. Those get emitted via ActivityDescriptor.
     * We do the check here so that we can also cache the cost of calling
     * {@link FragmentCompatUtil#isDialogFragment(Object)}.
     */

    Object fragment = FragmentCompatUtil.findFragmentForView(childView);
    if (fragment != null && !FragmentCompatUtil.isDialogFragment(fragment)) {
      mViewToElementMap.put(childView, fragment);
      return fragment;
    } else {
      mViewToElementMap.put(childView, childView);
      return childView;
    }
  }
}

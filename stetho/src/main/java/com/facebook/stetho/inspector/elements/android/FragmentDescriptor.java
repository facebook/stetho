/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.graphics.Rect;
import android.view.View;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.android.FragmentAccessor;
import com.facebook.stetho.common.android.FragmentCompat;
import com.facebook.stetho.common.android.ResourcesUtil;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.DescriptorMap;

import javax.annotation.Nullable;

final class FragmentDescriptor
    extends AbstractChainedDescriptor<Object> implements HighlightableDescriptor<Object> {
  private static final String ID_ATTRIBUTE_NAME = "id";
  private static final String TAG_ATTRIBUTE_NAME = "tag";

  private final FragmentAccessor mAccessor;

  public static DescriptorMap register(DescriptorMap map) {
    maybeRegister(map, FragmentCompat.getSupportLibInstance());
    maybeRegister(map, FragmentCompat.getFrameworkInstance());
    return map;
  }

  private static void maybeRegister(DescriptorMap map, @Nullable FragmentCompat compat) {
    if (compat != null) {
      Class<?> fragmentClass = compat.getFragmentClass();
      LogUtil.d("Adding support for %s", fragmentClass.getName());
      map.registerDescriptor(fragmentClass, new FragmentDescriptor(compat));
    }
  }

  private FragmentDescriptor(FragmentCompat compat) {
    mAccessor = compat.forFragment();
  }

  @Override
  protected void onGetAttributes(Object element, AttributeAccumulator attributes) {
    int id = mAccessor.getId(element);
    if (id != FragmentAccessor.NO_ID) {
      String value = ResourcesUtil.getIdStringQuietly(
          element,
          mAccessor.getResources(element),
          id);
      attributes.store(ID_ATTRIBUTE_NAME, value);
    }

    String tag = mAccessor.getTag(element);
    if (tag != null && tag.length() > 0) {
      attributes.store(TAG_ATTRIBUTE_NAME, tag);
    }
  }

  @Override
  protected void onGetChildren(Object element, Accumulator<Object> children) {
    View view = mAccessor.getView(element);
    if (view != null) {
      children.store(view);
    }
  }

  @Override
  @Nullable
  public View getViewAndBoundsForHighlighting(Object element, Rect bounds) {
    return mAccessor.getView(element);
  }

  @Nullable
  @Override
  public Object getElementToHighlightAtPosition(Object element, int x, int y, Rect bounds) {
    final Descriptor.Host host = getHost();
    View view = null;
    HighlightableDescriptor descriptor = null;

    if (host instanceof AndroidDescriptorHost) {
      view = mAccessor.getView(element);
      descriptor = ((AndroidDescriptorHost) host).getHighlightableDescriptor(view);
    }

    return descriptor == null
        ? null
        : descriptor.getElementToHighlightAtPosition(view, x, y, bounds);
  }
}

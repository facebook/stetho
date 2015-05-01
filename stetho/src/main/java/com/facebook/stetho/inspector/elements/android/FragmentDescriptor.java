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

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.android.FragmentAccessor;
import com.facebook.stetho.common.android.FragmentCompat;
import com.facebook.stetho.common.android.ResourcesUtil;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.DescriptorMap;

import javax.annotation.Nullable;

final class FragmentDescriptor
    extends ChainedDescriptor<Object> implements HighlightableDescriptor {
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
      map.register(fragmentClass, new FragmentDescriptor(compat));
    }
  }

  private FragmentDescriptor(FragmentCompat compat) {
    mAccessor = compat.forFragment();
  }

  @Override
  protected void onCopyAttributes(Object element, AttributeAccumulator attributes) {
    int id = mAccessor.getId(element);
    if (id != FragmentAccessor.NO_ID) {
      String value = ResourcesUtil.getIdStringQuietly(
          element,
          mAccessor.getResources(element),
          id);
      attributes.add(ID_ATTRIBUTE_NAME, value);
    }

    String tag = mAccessor.getTag(element);
    if (tag != null && tag.length() > 0) {
      attributes.add(TAG_ATTRIBUTE_NAME, tag);
    }
  }

  @Override
  protected int onGetChildCount(Object element) {
    View view = mAccessor.getView(element);
    return (view == null) ? 0 : 1;
  }

  @Override
  protected Object onGetChildAt(Object element, int index) {
    if (index != 0) {
      throw new IndexOutOfBoundsException();
    }

    View view = mAccessor.getView(element);
    if (view == null) {
      throw new IndexOutOfBoundsException();
    }

    return view;
  }

  @Override
  public View getViewForHighlighting(Object element) {
    return mAccessor.getView(element);
  }
}

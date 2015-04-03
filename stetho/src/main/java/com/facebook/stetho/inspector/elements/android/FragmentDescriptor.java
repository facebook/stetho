// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.view.View;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.android.FragmentAccessor;
import com.facebook.stetho.common.android.FragmentApi;
import com.facebook.stetho.common.android.ResourcesUtil;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.DescriptorMap;

final class FragmentDescriptor
    extends ChainedDescriptor<Object> implements HighlightableDescriptor {
  private static final String ID_ATTRIBUTE_NAME = "id";
  private static final String TAG_ATTRIBUTE_NAME = "tag";

  public static DescriptorMap register(DescriptorMap map) {
    Class<?> supportFragmentClass = FragmentApi.tryGetSupportFragmentClass();
    if (supportFragmentClass != null) {
      LogUtil.d("Registering support Fragment descriptor");
      map.register(supportFragmentClass, new FragmentDescriptor());
    }

    Class<?> fragmentClass = FragmentApi.tryGetFragmentClass();
    if (fragmentClass != null) {
      LogUtil.d("Registering Fragment descriptor");
      map.register(fragmentClass, new FragmentDescriptor());
    }

    return map;
  }

  private FragmentDescriptor() {
  }

  @Override
  protected void onCopyAttributes(Object element, AttributeAccumulator attributes) {
    FragmentAccessor accessor = FragmentApi.getFragmentAccessorFor(element);

    int id = accessor.getId(element);
    if (id != FragmentAccessor.NO_ID) {
      String value = ResourcesUtil.getIdStringQuietly(
          element,
          accessor.getResources(element),
          id);
      attributes.add(ID_ATTRIBUTE_NAME, value);
    }

    String tag = accessor.getTag(element);
    if (tag != null && tag.length() > 0) {
      attributes.add(TAG_ATTRIBUTE_NAME, tag);
    }
  }

  @Override
  protected int onGetChildCount(Object element) {
    FragmentAccessor accessor = FragmentApi.getFragmentAccessorFor(element);
    View view = accessor.getView(element);
    return (view == null) ? 0 : 1;
  }

  @Override
  protected Object onGetChildAt(Object element, int index) {
    if (index != 0) {
      throw new IndexOutOfBoundsException();
    }

    FragmentAccessor accessor = FragmentApi.getFragmentAccessorFor(element);
    View view = accessor.getView(element);
    if (view == null) {
      throw new IndexOutOfBoundsException();
    }

    return view;
  }

  @Override
  public View getViewForHighlighting(Object element) {
    FragmentAccessor accessor = FragmentApi.getFragmentAccessorFor(element);
    return accessor.getView(element);
  }
}

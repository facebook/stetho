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

import com.facebook.stetho.common.StringUtil;
import com.facebook.stetho.common.android.ResourcesUtil;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;

import javax.annotation.Nullable;

final class ViewDescriptor extends ChainedDescriptor<View> implements HighlightableDescriptor {
  private static final String ID_ATTRIBUTE_NAME = "id";

  @Override
  protected String onGetNodeName(View element) {
    String className = element.getClass().getName();

    return
        StringUtil.removePrefix(className, "android.view.",
        StringUtil.removePrefix(className, "android.widget."));
  }

  @Override
  protected void onCopyAttributes(View element, AttributeAccumulator attributes) {
    String id = getIdAttribute(element);
    if (id != null) {
      attributes.add(ID_ATTRIBUTE_NAME, id);
    }
  }

  @Nullable
  private static String getIdAttribute(View element) {
    int id = element.getId();
    if (id == View.NO_ID) {
      return null;
    }
    return ResourcesUtil.getIdStringQuietly(element, element.getResources(), id);
  }

  @Override
  public View getViewForHighlighting(Object element) {
    return (View)element;
  }
}

// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.view.View;

import com.facebook.stetho.common.StringUtil;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;

final class ViewDescriptor extends ChainedDescriptor<View> {
  @Override
  protected String onGetNodeName(View element) {
    String className = element.getClass().getName();

    return
        StringUtil.removePrefix(className, "android.view.",
        StringUtil.removePrefix(className, "android.widget."));
  }
}


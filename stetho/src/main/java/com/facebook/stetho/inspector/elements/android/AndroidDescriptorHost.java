// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.view.View;

import com.facebook.stetho.inspector.elements.Descriptor;

import javax.annotation.Nullable;

interface AndroidDescriptorHost extends Descriptor.Host {
  @Nullable
  public View getHighlightingView(@Nullable Object element);
}

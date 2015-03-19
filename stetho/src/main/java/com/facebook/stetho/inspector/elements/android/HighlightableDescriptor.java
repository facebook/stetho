// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.view.View;

import javax.annotation.Nullable;

interface HighlightableDescriptor {
  @Nullable
  public View getViewForHighlighting(Object element);
}

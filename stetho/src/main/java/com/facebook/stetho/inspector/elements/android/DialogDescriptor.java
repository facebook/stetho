/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Dialog;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.Descriptor;

import javax.annotation.Nullable;

final class DialogDescriptor
    extends AbstractChainedDescriptor<Dialog> implements HighlightableDescriptor<Dialog> {
  @Override
  protected void onGetChildren(Dialog element, Accumulator<Object> children) {
    Window window = element.getWindow();
    if (window != null) {
      children.store(window);
    }
  }

  @Nullable
  @Override
  public View getViewAndBoundsForHighlighting(Dialog element, Rect bounds) {
    final Descriptor.Host host = getHost();
    Window window = null;
    HighlightableDescriptor descriptor = null;

    if (host instanceof AndroidDescriptorHost) {
      window = element.getWindow();
      descriptor = ((AndroidDescriptorHost) host).getHighlightableDescriptor(window);
    }

    return descriptor == null
        ? null
        : descriptor.getViewAndBoundsForHighlighting(window, bounds);
  }

  @Nullable
  @Override
  public Object getElementToHighlightAtPosition(Dialog element, int x, int y, Rect bounds) {
    final Descriptor.Host host = getHost();
    Window window = null;
    HighlightableDescriptor descriptor = null;

    if (host instanceof AndroidDescriptorHost) {
      window = element.getWindow();
      descriptor = ((AndroidDescriptorHost) host).getHighlightableDescriptor(window);
    }

    return descriptor == null
        ? null
        : descriptor.getElementToHighlightAtPosition(window, x, y, bounds);
  }
}

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
import android.view.Window;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.Descriptor;

import javax.annotation.Nullable;

final class WindowDescriptor extends AbstractChainedDescriptor<Window>
    implements HighlightableDescriptor<Window> {
  @Override
  protected void onGetChildren(Window element, Accumulator<Object> children) {
    View decorView = element.peekDecorView();
    if (decorView != null) {
      children.store(decorView);
    }
  }

  @Override
  @Nullable
  public View getViewAndBoundsForHighlighting(Window element, Rect bounds) {
    return element.peekDecorView();
  }

  @Nullable
  @Override
  public Object getElementToHighlightAtPosition(Window element, int x, int y, Rect bounds) {
    final Descriptor.Host host = getHost();
    View view = null;
    HighlightableDescriptor descriptor = null;

    if (host instanceof AndroidDescriptorHost) {
      view = element.peekDecorView();
      descriptor = ((AndroidDescriptorHost) host).getHighlightableDescriptor(view);
    }

    return descriptor == null
        ? null
        : descriptor.getElementToHighlightAtPosition(view, x, y, bounds);
  }
}

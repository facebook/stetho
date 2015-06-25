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
import android.view.Window;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.Descriptor;

import javax.annotation.Nullable;

final class WindowDescriptor extends ChainedDescriptor<Window> implements HighlightableDescriptor {
  @Override
  protected void onGetChildren(Window element, Accumulator<Object> children) {
    View decorView = element.peekDecorView();
    if (decorView != null) {
      children.store(decorView);
      registerDecorView(decorView);
    }
  }

  @Override
  @Nullable
  public View getViewForHighlighting(Object element) {
    Window window = (Window) element;
    return window.peekDecorView();
  }

  private void registerDecorView(View decorView) {
    if (decorView instanceof ViewGroup) {
      Descriptor descriptor = getHost().getDescriptor(decorView);
      if (descriptor instanceof ViewGroupDescriptor) {
        ((ViewGroupDescriptor) descriptor).registerDecorView((ViewGroup) decorView);
      }
    }
  }
}

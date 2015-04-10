/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.ThreadBound;

import javax.annotation.Nullable;

public interface DOMProvider extends ThreadBound {
  public void setListener(Listener listener);

  public void dispose();

  @Nullable
  public Object getRootElement();

  @Nullable
  public NodeDescriptor getNodeDescriptor(@Nullable Object element);

  public void highlightElement(Object element, int color);

  public void hideHighlight();

  public void setInspectModeEnabled(boolean enabled);

  public void setAttributesAsText(Object element, String text);

  public static interface Factory {
    DOMProvider create();
  }

  public static interface Listener {
    public void onAttributeModified(
        Object element,
        String name,
        String value);

    public void onAttributeRemoved(
        Object element,
        String name);

    public void onChildInserted(
        Object parentElement,
        @Nullable Object previousElement,
        Object childElement);

    public void onChildRemoved(
        Object parentElement,
        Object childElement);

    public void onInspectRequested(
        Object element);
  }
}

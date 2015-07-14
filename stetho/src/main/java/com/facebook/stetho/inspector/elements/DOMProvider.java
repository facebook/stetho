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
  void setListener(Listener listener);

  void dispose();

  @Nullable
  Object getRootElement();

  @Nullable
  NodeDescriptor getNodeDescriptor(@Nullable Object element);

  void highlightElement(Object element, int color);

  void hideHighlight();

  void setInspectModeEnabled(boolean enabled);

  void setAttributesAsText(Object element, String text);

  interface Factory {
    DOMProvider create();
  }

  interface Listener {
    void onPossiblyChanged();

    void onAttributeModified(
        Object element,
        String name,
        String value);

    void onAttributeRemoved(
        Object element,
        String name);

    void onInspectRequested(
        Object element);
  }
}

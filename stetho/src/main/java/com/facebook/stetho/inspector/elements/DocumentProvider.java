/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.ThreadBound;

import javax.annotation.Nullable;

/**
 * Provides a document that can be rendered in Chrome's Elements tab (conforming loosely to the
 * W3C DOM to the degree specified in this API).
 *
 * @see DocumentProviderFactory
 */
public interface DocumentProvider extends ThreadBound {
  void setListener(DocumentProviderListener listener);

  void dispose();

  @Nullable
  Object getRootElement();

  @Nullable
  NodeDescriptor getNodeDescriptor(@Nullable Object element);

  void highlightElement(Object element, int color);

  void hideHighlight();

  void setInspectModeEnabled(boolean enabled);

  void setAttributesAsText(Object element, String text);
}

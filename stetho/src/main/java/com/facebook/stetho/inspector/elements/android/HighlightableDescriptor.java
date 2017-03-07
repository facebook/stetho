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

import javax.annotation.Nullable;

public interface HighlightableDescriptor<E> {

  /**
   * Return the {@link View} to highlight or null if this element cannot be highlighted.
   * If the element does not span the full bounds of the returned {@link View} you can set
   * the bounds of the passed in Rect. By default the passed in bounds are empty which means
   * highlight the full bounds of the {@link View}.
   */
  @Nullable
  View getViewAndBoundsForHighlighting(E element, Rect bounds);

  /**
   * Used when activating find by touch feature to figure out which element to focus / highlight.
   *
   * @param element the element
   * @param y coordinate in local coordinate space
   * @param y coordinate in local coordinate space
   * @param bounds The bounds of the returned element. Used to offset the coordinates for next call.
   * @return A child element or self if this coordinate falls within self and not a child.
   */
  @Nullable
  Object getElementToHighlightAtPosition(E element, int x, int y, Rect bounds);
}

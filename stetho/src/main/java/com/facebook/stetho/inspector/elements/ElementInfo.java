/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.ListUtil;
import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.List;

@Immutable
public final class ElementInfo {
  public final Object element;
  public @Nullable final Object parentElement;
  public final List<Object> children;

  ElementInfo(
      Object element,
      @Nullable Object parentElement,
      List<Object> children) {
    this.element = Util.throwIfNull(element);
    this.parentElement = parentElement;
    this.children = ListUtil.copyToImmutableList(children);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o instanceof ElementInfo) {
      ElementInfo other = (ElementInfo) o;
      return this.element == other.element
          && this.parentElement == other.parentElement
          && ListUtil.identityEquals(this.children, other.children);
    }

    return false;
  }
}

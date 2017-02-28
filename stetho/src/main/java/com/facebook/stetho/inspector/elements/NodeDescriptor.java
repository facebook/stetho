/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ThreadBound;

import javax.annotation.Nullable;

public interface NodeDescriptor<E> extends ThreadBound {
  void hook(E element);

  void unhook(E element);

  NodeType getNodeType(E element);

  String getNodeName(E element);

  String getLocalName(E element);

  @Nullable
  String getNodeValue(E element);

  void getChildren(E element, Accumulator<Object> children);

  void getAttributes(E element, AttributeAccumulator attributes);

  void setAttributesAsText(E element, String text);

  void getStyleRuleNames(E element, StyleRuleNameAccumulator accumulator);

  void getStyles(E element, String ruleName, StyleAccumulator accumulator);

  void setStyle(E element, String ruleName, String name, String value);

  void getComputedStyles(E element, ComputedStyleAccumulator accumulator);
}

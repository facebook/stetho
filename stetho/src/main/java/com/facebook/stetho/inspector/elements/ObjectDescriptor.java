/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.Accumulator;

public final class ObjectDescriptor extends Descriptor<Object> {
  @Override
  public void hook(Object element) {
  }

  @Override
  public void unhook(Object element) {
  }

  @Override
  public NodeType getNodeType(Object element) {
    return NodeType.ELEMENT_NODE;
  }

  @Override
  public String getNodeName(Object element) {
    return element.getClass().getName();
  }

  @Override
  public String getLocalName(Object element) {
    return getNodeName(element);
  }

  @Override
  public String getNodeValue(Object element) {
    return null;
  }

  @Override
  public void getChildren(Object element, Accumulator<Object> children) {
  }

  @Override
  public void getAttributes(Object element, AttributeAccumulator attributes) {
  }

  @Override
  public void setAttributesAsText(Object element, String text) {
  }

  @Override
  public void getStyleRuleNames(Object element, StyleRuleNameAccumulator accumulator) {
  }

  @Override
  public void getStyles(Object element, String ruleName, StyleAccumulator accumulator) {
  }

  @Override
  public void setStyle(Object element, String ruleName, String name, String value) {
  }

  @Override
  public void getComputedStyles(Object element, ComputedStyleAccumulator accumulator) {
  }
}

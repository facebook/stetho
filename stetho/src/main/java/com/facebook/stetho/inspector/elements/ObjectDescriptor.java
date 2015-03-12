// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements;

public final class ObjectDescriptor extends Descriptor {
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
  public int getChildCount(Object element) {
    return 0;
  }

  @Override
  public Object getChildAt(Object element, int index) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  public void copyAttributes(Object element, AttributeAccumulator attributes) {
  }
}
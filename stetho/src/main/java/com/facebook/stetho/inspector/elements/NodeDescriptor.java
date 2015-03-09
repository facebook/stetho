package com.facebook.stetho.inspector.elements;

import javax.annotation.Nullable;

public interface NodeDescriptor {
  public void hook(Object element);

  public void unhook(Object element);

  public NodeType getNodeType(Object element);

  public String getNodeName(Object element);

  public String getLocalName(Object element);

  @Nullable
  public String getNodeValue(Object element);

  public int getChildCount(Object element);

  public Object getChildAt(Object element, int index);

  public int getAttributeCount(Object element);

  public void copyAttributeAt(Object element, int index, NodeAttribute outAttribute);
}
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

public interface NodeDescriptor extends ThreadBound {
  public void hook(Object element);

  public void unhook(Object element);

  public NodeType getNodeType(Object element);

  public String getNodeName(Object element);

  public String getLocalName(Object element);

  @Nullable
  public String getNodeValue(Object element);

  public int getChildCount(Object element);

  public Object getChildAt(Object element, int index);

  public void copyAttributes(Object element, AttributeAccumulator attributes);

  public void setAttributesAsText(Object element, String text);
}

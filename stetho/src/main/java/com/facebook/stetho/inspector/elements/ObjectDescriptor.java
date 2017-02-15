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

import java.util.Map;
import java.util.HashMap;

public final class ObjectDescriptor extends Descriptor<Object> {
  private static Map<Integer, NodeID> sElementToIdMap = new HashMap<>();

  @Override
  public void hook(Object element) {
  }

  @Override
  public void unhook(Object element) {
    sElementToIdMap.remove(System.identityHashCode(element));
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
  public void getStyles(Object element, StyleAccumulator accumulator) {
  }

  @Override
  public void getAccessibilityStyles(Object element, StyleAccumulator accumulator) {
  }

  @Override
  public NodeID getNodeID(Object element) {
    final int key = System.identityHashCode(element);
    NodeID nodeID = sElementToIdMap.get(key);
    if (nodeID == null) {
      nodeID = new NodeID();
      sElementToIdMap.put(key, nodeID);
    }
    return nodeID;
  }
}

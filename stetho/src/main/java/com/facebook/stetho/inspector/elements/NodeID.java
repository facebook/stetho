/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import java.util.concurrent.atomic.AtomicInteger;

public class NodeID implements Comparable<NodeID> {
  private static AtomicInteger sNextValue = new AtomicInteger(0);
  public final int value;

  NodeID(int value) {
    this.value = value;
  }

  public NodeID() {
    this(sNextValue.getAndIncrement());
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o instanceof NodeID) {
      NodeID other = (NodeID) o;
      return this.value == other.value;
    }

    return false;
  }

  @Override
  public int compareTo(NodeID o) {
    return Integer.compare(this.value, o.value);
  }
}

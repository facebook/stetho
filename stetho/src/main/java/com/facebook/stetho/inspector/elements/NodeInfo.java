/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.ListUtil;
import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.List;

@Immutable
public final class NodeInfo {
  public final NodeID nodeID;
  public @Nullable final NodeID parentID;
  public final List<NodeID> childrenIDs;

  NodeInfo(
      NodeID nodeID,
      @Nullable NodeID parentID,
      List<NodeID> childrenIDs) {
    this.nodeID = Util.throwIfNull(nodeID);
    this.parentID = parentID;
    this.childrenIDs = ListUtil.copyToImmutableList(childrenIDs);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o instanceof NodeInfo) {
      NodeInfo other = (NodeInfo) o;
      return this.nodeID.value == other.nodeID.value
          && this.parentID.value == other.parentID.value
          && this.childrenIDs.equals(other.childrenIDs);
    }

    return false;
  }
}

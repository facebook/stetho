/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.network;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.util.HashMap;
import java.util.Map;

public class DefaultAsyncPrettyPrinterRegistry implements AsyncPrettyPrinterRegistry {

  @GuardedBy("this")
  private final Map<String, AsyncPrettyPrinterFactory> mRegistry = new HashMap<>();
  
  public synchronized boolean register(String headerName, AsyncPrettyPrinterFactory factory) {
    if (mRegistry.containsKey(headerName)) {
      return false;
    }
    mRegistry.put(headerName, factory);
    return true;
  }

  @Nullable
  public synchronized AsyncPrettyPrinterFactory lookup(String headerName) {
    return mRegistry.get(headerName);
  }

  public synchronized void unregister(String headerName) {
    mRegistry.remove(headerName);
  }
}

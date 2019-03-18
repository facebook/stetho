/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ThreadSafe
public class AsyncPrettyPrinterRegistry {

  private final Map<String, AsyncPrettyPrinterFactory> mRegistry = new HashMap<>();

  public synchronized void register(String headerName, AsyncPrettyPrinterFactory factory) {
    mRegistry.put(headerName, factory);
  }

  @Nullable
  public synchronized AsyncPrettyPrinterFactory lookup(String headerName) {
    return mRegistry.get(headerName);
  }

  public synchronized boolean unregister(String headerName) {
    return mRegistry.remove(headerName) != null;
  }
}

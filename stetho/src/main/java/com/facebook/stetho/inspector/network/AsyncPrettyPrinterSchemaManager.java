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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class AsyncPrettyPrinterSchemaManager {
  private static AsyncPrettyPrinterSchemaManager sSchemaManager;
  private Map<URL, String> mSchemaCache;

  private static void ensureInitialized() {
    if (sSchemaManager == null) {
      sSchemaManager = new AsyncPrettyPrinterSchemaManager(new HashMap<URL, String>());
    }
  }

  private AsyncPrettyPrinterSchemaManager(Map<URL, String> schemaCache) {
    mSchemaCache = schemaCache;
  }

  public synchronized void put(URL schemaUrl, String schema) {
    mSchemaCache.put(schemaUrl, schema);
  }

  @Nullable
  public synchronized String get(URL schemaUrl) {
    return mSchemaCache.get(schemaUrl);
  }

  public static AsyncPrettyPrinterSchemaManager getInstance() {
    ensureInitialized();
    return sSchemaManager;
  }

  public void clearCache() {
    mSchemaCache = new HashMap<>();
  }
}

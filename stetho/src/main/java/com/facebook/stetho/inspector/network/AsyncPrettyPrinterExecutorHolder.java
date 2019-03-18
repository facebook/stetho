/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

import javax.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A holder class for the executor service used for pretty printing related tasks
 */
final class AsyncPrettyPrinterExecutorHolder {

  private static ExecutorService sExecutorService;

  private AsyncPrettyPrinterExecutorHolder() {
  }
  
  public static void ensureInitialized() {
    if (sExecutorService == null) {
      sExecutorService = Executors.newCachedThreadPool();
    }
  }

  @Nullable
  public static ExecutorService getExecutorService() {
    return sExecutorService;
  }

  public static void shutdown() {
    sExecutorService.shutdown();
    sExecutorService = null;
  }
}

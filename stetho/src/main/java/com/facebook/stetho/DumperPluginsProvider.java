/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho;

import com.facebook.stetho.dumpapp.DumperPlugin;

/**
 * Provider interface to lazily supply dumpers to be initialized on demand.  It is critical
 * that the main initialization flow of Stetho perform no actual work beyond simply
 * binding a socket and starting the listener thread.
 */
public interface DumperPluginsProvider {
  Iterable<DumperPlugin> get();
}

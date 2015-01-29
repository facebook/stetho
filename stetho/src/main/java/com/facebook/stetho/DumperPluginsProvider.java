// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho;

import com.facebook.stetho.dumpapp.DumperPlugin;

/**
 * Provider interface to lazily supply dumpers to be initialized on demand.  It is critical
 * that the main initialization flow of Stetho perform no actual work beyond simply
 * binding a socket and starting the listener thread.
 */
public interface DumperPluginsProvider {
  public Iterable<DumperPlugin> get();
}

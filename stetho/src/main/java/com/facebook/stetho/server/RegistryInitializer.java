/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.server;

import org.apache.http.protocol.HttpRequestHandlerRegistry;

/**
 * This is implemented by code that uses {@link LocalSocketHttpServer} to lazy-initialize
 * the {@link HttpRequestHandlerRegistry} after the first client has connected.
 * <p/>
 * Initializing a registry allocates and class-loads a lot, and we would prefer to avoid
 * all that in the common case that Stetho is not used.
 */
public interface RegistryInitializer {

  /**
   * Creates the {@link HttpRequestHandlerRegistry} on-demand.
   * <p/>
   * This is only called once.
   */
  public HttpRequestHandlerRegistry getRegistry();
}

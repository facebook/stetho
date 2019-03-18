/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

/**
 * Interface that is called if AsyncPrettyPrinterRegistry is unpopulated when
 * the first peer connects to Stetho. It is responsible for registering header
 * names and their corresponding pretty printers
 */
public interface AsyncPrettyPrinterInitializer {

  /**
   * Populates AsyncPrettyPrinterRegistry with header names and their corresponding pretty
   * printers. This is responsible for registering all {@link AsyncPrettyPrinter} to the
   * provided registry.
   * @param registry
   */
  void populatePrettyPrinters(AsyncPrettyPrinterRegistry registry);

}

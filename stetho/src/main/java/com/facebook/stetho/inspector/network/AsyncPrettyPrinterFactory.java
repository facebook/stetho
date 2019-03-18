/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

import com.facebook.stetho.inspector.protocol.module.Page;

/**
 * Interface for creating a factory for asynchronous pretty printers
 */
public interface AsyncPrettyPrinterFactory {

  /**
   * Creates an asynchronous pretty printer. This method must not be blocking.
   *
   * @param headerName header name of a response which is used to associate
   * with an asynchronous pretty printer
   * @param headerValue header value of a response which contains the URI for
   * the schema data needed to pretty print the response body
   * @return an asynchronous pretty printer to prettify the response body
   */
  public AsyncPrettyPrinter getInstance(String headerName, String headerValue);
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * Interface that callers need to implement in order to pretty print binary payload received by Stetho
 */
public interface AsyncPrettyPrinter {
  /**
   * Prints the prettified version of payload to output. This method can block
   * for a certain period of time. Note that Stetho may impose arbitrary
   * time out on this method.
   *
   * @param output Writes the prettified version of payload
   * @param payload Response stream that has the raw data to be prettified
   * @throws IOException
   */
  public void printTo(PrintWriter output, InputStream payload) throws IOException;

  /**
   * Specifies the type of pretty printed content. Note that this method is called
   * before the content is actually pretty printed. Stetho uses this
   * method to make a hopeful guess of the type of prettified content
   *
   * @return an enum defined by PrettyPrinterDisplayType class
   */
  public PrettyPrinterDisplayType getPrettifiedType();
}

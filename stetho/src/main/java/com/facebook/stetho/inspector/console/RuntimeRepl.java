/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.console;

public interface RuntimeRepl {
  Object evaluate(String expression) throws Throwable;

  /**
   * Assign the variable in the evaluation scope such that it can be accessed by subsequent
   * evaluations.  The value object must be strong referenced by the implementor!

   * @param varName Variable name to assign
   * @param value Value of the object
   * @return True if assignment is supported; false otherwise
   * @throws Throwable Thrown if there is an error evaluating the variable name
   */
  boolean assignVariable(String varName, Object value) throws Throwable;
}

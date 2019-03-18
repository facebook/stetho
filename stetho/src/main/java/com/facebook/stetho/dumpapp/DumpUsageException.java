/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.dumpapp;

/**
 * Usage error in a {@link DumperPlugin}.
 */
public class DumpUsageException extends DumpException {
  public DumpUsageException(String message) {
    super(message);
  }
}

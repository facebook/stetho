// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.dumpapp;

/**
 * Usage error in a {@link DumperPlugin}.
 */
public class DumpUsageException extends DumpException {
  public DumpUsageException(String message) {
    super(message);
  }
}

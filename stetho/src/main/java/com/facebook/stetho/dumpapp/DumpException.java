// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.dumpapp;

import org.apache.commons.cli.ParseException;

/**
 * Exception thrown if there is a functional issue executing the specified commands.  This is
 * not thrown on {@link ParseException} (which represents a command-line syntax issue).
 * <p>
 * This exception's message should be human readable and will be printed to the dumpapp
 * caller.  dumpapp will also exit with a non-zero exit code.
 */
public class DumpException extends Exception {
  public DumpException(String message) {
    super(message);
  }
}

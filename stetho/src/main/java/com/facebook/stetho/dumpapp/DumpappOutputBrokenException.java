// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.dumpapp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.http.impl.io.ChunkedOutputStream;

/**
 * When streaming output, it is common for the user to just hit Ctrl-C
 * to terminate the stream.  When this happens, the underlying output
 * stream throws an {@link IOException} to indicate the pipe is broken.
 * Dumpapp uses a {@link PrintStream} to wrap the underlying {@link OutputStream}
 * though, and {@link PrintStream} silently swallows {@link IOException}.
 * <p/>
 * There are stream implementations, such as {@link ChunkedOutputStream}
 * that become corrupt if they throw from {@link OutputStream#write} and
 * write is called again.
 * <p/>
 * While streaming dumpers can/should check {@link PrintStream#checkError},
 * this is used in cases where we know the stream has gone bad to force flow
 * control out of the dumper and back into the calling machinery that controls
 * the stream framer.
 */
public class DumpappOutputBrokenException extends RuntimeException {

  public DumpappOutputBrokenException() {
  }

  public DumpappOutputBrokenException(String detailMessage) {
    super(detailMessage);
  }

  public DumpappOutputBrokenException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  public DumpappOutputBrokenException(Throwable throwable) {
    super(throwable);
  }
}

// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {
  public static <T> T throwIfNull(T item) {
    if (item == null) {
      throw new NullPointerException();
    }
    return item;
  }

  public static void throwIfNotNull(Object item) {
    if (item != null) {
      throw new IllegalStateException();
    }
  }

  public static void throwIfNot(boolean condition) {
    if (!condition) {
      throw new IllegalStateException();
    }
  }

  public static void throwIfNot(boolean condition, String format, Object...args) {
    if (!condition) {
      String message = String.format(format, args);
      throw new IllegalStateException(message);
    }
  }

  public static void copy(InputStream input, OutputStream output, byte[] buffer)
      throws IOException {
    int n;
    while ((n = input.read(buffer)) != -1) {
      output.write(buffer, 0, n);
    }
  }

  public static void close(Closeable closeable, boolean hideException) throws IOException {
    if (closeable != null) {
      if (hideException) {
        try {
          closeable.close();
        } catch (IOException e) {
          LogUtil.e(e, "Hiding IOException because another is pending");
        }
      } else {
        closeable.close();
      }
    }
  }

  public static void sleepUninterruptibly(long millis) {
    long remaining = millis;
    long startTime = System.currentTimeMillis();
    do {
      try {
        Thread.sleep(remaining);
        return;
      } catch (InterruptedException e) {
        long sleptFor = System.currentTimeMillis() - startTime;
        remaining -= sleptFor;
      }
    } while (remaining > 0);
  }
}

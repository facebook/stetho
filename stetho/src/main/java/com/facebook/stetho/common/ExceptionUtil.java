/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.common;

public class ExceptionUtil {
  @SuppressWarnings("unchecked")
  public static <T extends Throwable> void propagateIfInstanceOf(Throwable t, Class<T> type)
      throws T {
    if (type.isInstance(t)) {
      throw (T)t;
    }
  }

  public static RuntimeException propagate(Throwable t) {
    propagateIfInstanceOf(t, Error.class);
    propagateIfInstanceOf(t, RuntimeException.class);
    throw new RuntimeException(t);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
    throw (T)t;
  }
}

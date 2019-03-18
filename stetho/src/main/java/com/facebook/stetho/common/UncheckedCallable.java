/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.common;

/**
 * A task that returns a result. Implementers define a single method with no arguments called
 * {@code call}.
 *
 * <p>This interface is identical to {@link java.util.concurrent.Callable} but without the checked
 * exception.
 *
 * @param <V> the result type of method {@code call}
 */
public interface UncheckedCallable<V> {
  V call();
}

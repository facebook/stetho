/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.common;

import java.util.ArrayList;

public final class ArrayListAccumulator<E> extends ArrayList<E> implements Accumulator<E> {
  @Override
  public void store(E object) {
    add(object);
  }
}

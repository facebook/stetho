/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common;

public class ManagedIntArray {
  private int[] mData;
  private int mSize;

  public ManagedIntArray(int startingCapacity) {
    mData = new int[startingCapacity];
  }

  public ManagedIntArray(ManagedIntArray a) {
    int size = a.size();
    mData = new int[size];
    System.arraycopy(a.array(), 0, mData, 0, size);
    mSize = size;
  }

  public void ensureCapacity(int capacity) {
    growIfNecessary(capacity);
  }

  public int[] array() {
    return mData;
  }

  public int size() {
    return mSize;
  }

  public void add(int value) {
    growIfNecessary(mSize + 1);
    mData[mSize] = value;
    mSize++;
  }

  public int get(int index) {
    return mData[index];
  }

  public void reverse() {
    int halfN = mSize / 2;
    for (int i = 0, j = mSize; i < halfN; i++, j--) {
      int tmp = mData[i];
      mData[i] = mData[j];
      mData[j] = tmp;
    }
  }

  public void clear() {
    mSize = 0;
  }

  private void growIfNecessary(int minCapacity) {
    int currentCapacity = mData.length;
    if (currentCapacity < minCapacity) {
      int desiredCapacity = currentCapacity;
      do {
        desiredCapacity *= 2;
      } while (desiredCapacity < minCapacity);

      int[] newData = new int[desiredCapacity];
      System.arraycopy(mData, 0, newData, 0, mSize);
      mData = newData;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ManagedIntArray that = (ManagedIntArray) o;

    if (mSize != that.mSize) return false;
    for (int i = 0; i < mSize; i++) {
      if (mData[i] != that.mData[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (int i = 0; i < mSize; i++) {
      result = 31 * result + mData[i];
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();

    b.append('[');
    if (mSize > 0) {
      b.append(mData[0]);
      for (int i = 1; i < mSize; i++) {
        b.append(", ");
        b.append(mData[i]);
      }
    }
    b.append(']');

    return b.toString();
  }
}

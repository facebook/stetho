/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class CountingOutputStream extends FilterOutputStream {
  private long mCount;

  public CountingOutputStream(OutputStream out) {
    super(out);
  }

  public long getCount() {
    return mCount;
  }

  @Override
  public void write(int oneByte) throws IOException {
    out.write(oneByte);
    mCount++;
  }

  @Override
  public void write(byte[] buffer) throws IOException {
    write(buffer, 0, buffer.length);
  }

  @Override
  public void write(byte[] buffer, int offset, int length) throws IOException {
    out.write(buffer, offset, length);
    mCount += length;
  }
}

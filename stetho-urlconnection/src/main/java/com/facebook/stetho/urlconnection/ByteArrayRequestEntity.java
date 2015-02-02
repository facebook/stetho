package com.facebook.stetho.urlconnection;

import java.io.IOException;
import java.io.OutputStream;

public class ByteArrayRequestEntity implements SimpleRequestEntity {
  private final byte[] mData;

  public ByteArrayRequestEntity(byte[] data) {
    mData = data;
  }

  @Override
  public void writeTo(OutputStream out) throws IOException {
    out.write(mData);
  }
}

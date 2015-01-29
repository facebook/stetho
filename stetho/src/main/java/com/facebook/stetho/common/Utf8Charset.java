// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.common;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Utf8Charset {

  public static final String NAME = "UTF-8";
  public static final Charset INSTANCE = Charset.forName(NAME);

  public static byte[] encodeUTF8(String str) {
    try {
      return str.getBytes(NAME);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}

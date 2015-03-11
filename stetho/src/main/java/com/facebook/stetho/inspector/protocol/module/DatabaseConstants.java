// Copyright 2015-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.protocol.module;

import android.os.Build;

public interface DatabaseConstants {

  /**
   * Minimum API version required to use the {@link Database}.
   */
  public static final int MIN_API_LEVEL = Build.VERSION_CODES.HONEYCOMB;
}

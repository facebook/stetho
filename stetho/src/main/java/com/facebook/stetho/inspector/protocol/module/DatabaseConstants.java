/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.os.Build;

public interface DatabaseConstants {

  /**
   * Minimum API version required to use the {@link Database}.
   */
  public static final int MIN_API_LEVEL = Build.VERSION_CODES.ICE_CREAM_SANDWICH;
}

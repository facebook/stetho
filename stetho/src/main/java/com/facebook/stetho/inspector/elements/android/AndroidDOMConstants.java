/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.os.Build;

public interface AndroidDOMConstants {
  /**
   * Minimum API version required to make effective use of the DOM module.  This can be moved
   * back significantly through manual APIs to discover {@link android.app.Activity} instances.
   */
  public static final int MIN_API_LEVEL = Build.VERSION_CODES.ICE_CREAM_SANDWICH;
}

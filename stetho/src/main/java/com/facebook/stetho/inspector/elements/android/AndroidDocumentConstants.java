/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements.android;

import android.os.Build;

public interface AndroidDocumentConstants {
  /**
   * Minimum API version required to make effective use of AndroidDocumentProvider. This can be
   * moved back significantly through manual APIs to discover {@link android.app.Activity}
   * instances.
   */
  int MIN_API_LEVEL = Build.VERSION_CODES.ICE_CREAM_SANDWICH;
}

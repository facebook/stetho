/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.console;

import com.facebook.stetho.inspector.helper.ChromePeerManager;

import javax.annotation.Nullable;

public class ConsolePeerManager extends ChromePeerManager {

  private static ConsolePeerManager sInstance;

  private ConsolePeerManager() {
    super();
  }

  @Nullable
  public static synchronized ConsolePeerManager getInstanceOrNull() {
    return sInstance;
  }

  public static synchronized ConsolePeerManager getOrCreateInstance() {
    if (sInstance == null) {
      sInstance = new ConsolePeerManager();
    }
    return sInstance;
  }
}

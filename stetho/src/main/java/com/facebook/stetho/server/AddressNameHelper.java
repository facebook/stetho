/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.server;

import com.facebook.stetho.common.ProcessUtil;

public class AddressNameHelper {
  private static final String PREFIX = "stetho_";

  public static String createCustomAddress(String suffix) {
    return
        PREFIX +
        ProcessUtil.getProcessName() +
        suffix;
  }
}

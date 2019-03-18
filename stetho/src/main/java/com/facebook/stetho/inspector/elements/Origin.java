/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.json.annotation.JsonValue;

public enum Origin {
  INJECTED("injected"),
  USER_AGENT("user-agent"),
  INSPECTOR("inspector"),
  REGULAR("regular");

  private final String mValue;

  Origin(String value) {
    mValue = value;
  }

  @JsonValue
  public String getProtocolValue() {
    return mValue;
  }
}

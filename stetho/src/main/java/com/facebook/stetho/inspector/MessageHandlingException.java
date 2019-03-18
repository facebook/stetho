/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector;

public class MessageHandlingException extends Exception {
  public MessageHandlingException(Throwable cause) {
    super(cause);
  }

  public MessageHandlingException(String message) {
    super(message);
  }
}

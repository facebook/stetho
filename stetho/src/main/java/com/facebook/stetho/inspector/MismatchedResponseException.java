/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector;

public class MismatchedResponseException extends MessageHandlingException {
  public long mRequestId;

  public MismatchedResponseException(long requestId) {
    super("Response for request id " + requestId + ", but no such request is pending");
    mRequestId = requestId;
  }

  public long getRequestId() {
    return mRequestId;
  }
}

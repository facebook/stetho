/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.jsonrpc;

import javax.annotation.Nullable;

/**
 * Represents an outstanding request to the peer (issued by us).  This callback will be
 * fired when the server responds.  Note that with JSON-RPC, there is a special kind of
 * request called a notification which does not require a callback (and thus won't use
 * this class).
 */
public class PendingRequest {
  public final long requestId;
  public final @Nullable PendingRequestCallback callback;

  public PendingRequest(long requestId, @Nullable PendingRequestCallback callback) {
    this.requestId = requestId;
    this.callback = callback;
  }
}

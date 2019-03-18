/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.content.Context;

/**
 * Replaces {@link Database.DatabaseDriver} to enforce that the generic type must
 * extend {@link DatabaseDescriptor}.
 */
public abstract class DatabaseDriver2<DESC extends DatabaseDescriptor>
    extends BaseDatabaseDriver<DESC> {
  public DatabaseDriver2(Context context) {
    super(context);
  }
}

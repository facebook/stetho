/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.database;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides the results of {@link Context#databaseList()} for {@link SqliteDatabaseDriver}.
 */
public final class DefaultDatabaseFilesProvider implements DatabaseFilesProvider {
  private final Context mContext;

  public DefaultDatabaseFilesProvider(Context context) {
    mContext = context;
  }

  @Override
  public List<File> getDatabaseFiles() {
    List<File> databaseFiles = new ArrayList<File>();
    for (String databaseName : mContext.databaseList()) {
      databaseFiles.add(mContext.getDatabasePath(databaseName));
    }
    return databaseFiles;
  }
}

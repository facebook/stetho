/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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

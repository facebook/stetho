/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.facebook.stetho.inspector.database.SQLiteDatabaseCompat.SQLiteOpenOptions;

import java.io.File;

/**
 * Opens the requested database using
 * {@link SQLiteDatabase#openDatabase(String, SQLiteDatabase.CursorFactory, int)} directly.
 *
 * <p>It is intended that this class be subclassed to enable/disable features via
 * {@link #determineOpenOptions(File)}</p>
 */
public class DefaultDatabaseConnectionProvider implements DatabaseConnectionProvider {
  public DefaultDatabaseConnectionProvider() {
  }

  @Override
  public SQLiteDatabase openDatabase(File databaseFile) throws SQLiteException {
    return performOpen(
        databaseFile,
        determineOpenOptions(databaseFile));
  }

  /**
   * Subclassing this function is intended to provide custom open behaviour on a per-file basis.
   */
  protected @SQLiteOpenOptions int determineOpenOptions(File databaseFile) {
    @SQLiteOpenOptions int flags = 0;

    // Try to guess if we should be using write-ahead logging.  If this heuristic fails
    // developers are expected to subclass this provider and explicitly assert the connection.
    File walFile = new File(databaseFile.getParent(), databaseFile.getName() + "-wal");
    if (walFile.exists()) {
      flags |= SQLiteDatabaseCompat.ENABLE_WRITE_AHEAD_LOGGING;
    }

    return flags;
  }

  /**
   * Perform the open per the options provided in {@link #determineOpenOptions(File)}.
   * Subclassing is supported however this typically indicates a missing feature of some kind
   * in {@link SQLiteDatabaseCompat} that should be patched in Stetho itself.
   */
  protected SQLiteDatabase performOpen(File databaseFile, @SQLiteOpenOptions int options) {
    int flags = SQLiteDatabase.OPEN_READWRITE;

    SQLiteDatabaseCompat compatInstance = SQLiteDatabaseCompat.getInstance();
    flags |= compatInstance.provideOpenFlags(options);

    SQLiteDatabase db = SQLiteDatabase.openDatabase(
        databaseFile.getAbsolutePath(),
        null /* cursorFactory */,
        flags);
    compatInstance.enableFeatures(options, db);
    return db;
  }
}

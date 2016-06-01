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

import java.io.File;

/**
 * Opens the requested database using
 * {@link SQLiteDatabase#openDatabase(String, SQLiteDatabase.CursorFactory, int)} directly.
 */
public class DefaultDatabaseConnectionProvider implements DatabaseConnectionProvider {
  public DefaultDatabaseConnectionProvider() {
  }

  @Override
  public SQLiteDatabase openDatabase(File databaseFile) throws SQLiteException {
    // Expected to throw if it cannot open the file (for example, if it doesn't exist).
    return SQLiteDatabase.openDatabase(
        databaseFile.getAbsolutePath(),
        null /* cursorFactory */,
        SQLiteDatabase.OPEN_READWRITE);
  }
}

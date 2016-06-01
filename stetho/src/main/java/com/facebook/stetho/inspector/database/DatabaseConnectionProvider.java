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
 * Provides a {@link SQLiteDatabase} connection for the specified database.  For use with
 * {@link SqliteDatabaseDriver}.
 */
public interface DatabaseConnectionProvider {
  /**
   * @param databaseFile Full path to the database file.
   * @return a connection for the specified databaseName.
   * @throws SQLiteException if there is an error opening the specified database
   */
  SQLiteDatabase openDatabase(File databaseFile) throws SQLiteException;
}

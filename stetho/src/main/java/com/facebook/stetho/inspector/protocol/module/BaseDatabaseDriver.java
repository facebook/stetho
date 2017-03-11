/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import java.util.List;

/**
 * Extend {@link DatabaseDriver2} directly.  This class is provided only as a common API compatible
 * base layer for the legacy {@link Database.DatabaseDriver}.
 */
public abstract class BaseDatabaseDriver<DESC> {

  protected Context mContext;

  public BaseDatabaseDriver(Context context) {
    mContext = context;
  }

  public Context getContext() {
    return mContext;
  }

  /**
   * Access a stable list of objects that describe the databases made available by this driver.
   * The list of returned objects must not change on each invocation as this will cause
   * a memory leak when assigning unique identifiers for the objects to remote peers.
   */
  public abstract List<DESC> getDatabaseNames();

  /**
   * Get or create a list of table names given a previously returned database descriptor instance
   * from {@link #getDatabaseNames()}.
   */
  public abstract List<String> getTableNames(DESC database);

  public abstract Database.ExecuteSQLResponse executeSQL(
      DESC database,
      String query,
      ExecuteResultHandler<Database.ExecuteSQLResponse> handler)
      throws SQLiteException;

  public interface ExecuteResultHandler<RESULT> {
    RESULT handleRawQuery() throws SQLiteException;

    RESULT handleSelect(Cursor result) throws SQLiteException;

    RESULT handleInsert(long insertedId) throws SQLiteException;

    RESULT handleUpdateDelete(int count) throws SQLiteException;
  }
}

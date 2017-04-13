/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.database;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.protocol.module.Database;
import com.facebook.stetho.inspector.protocol.module.DatabaseConstants;
import com.facebook.stetho.inspector.protocol.module.DatabaseDescriptor;
import com.facebook.stetho.inspector.protocol.module.DatabaseDriver2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class SqliteDatabaseDriver
    extends DatabaseDriver2<SqliteDatabaseDriver.SqliteDatabaseDescriptor> {
  private static final String[] UNINTERESTING_FILENAME_SUFFIXES = new String[]{
      "-journal",
      "-shm",
      "-uid",
      "-wal"
  };

  private final DatabaseFilesProvider mDatabaseFilesProvider;
  private final DatabaseConnectionProvider mDatabaseConnectionProvider;

  /**
   * Constructs the object with a {@link DatabaseFilesProvider} that supplies the database files
   * from {@link Context#databaseList()}.
   *
   * @param context the context
   * @deprecated use {@link SqliteDatabaseDriver#SqliteDatabaseDriver(Context, String, DatabaseFilesProvider, DatabaseConnectionProvider)}
   */
  @Deprecated
  public SqliteDatabaseDriver(Context context) {
    this(
        context,
        new DefaultDatabaseFilesProvider(context),
        new DefaultDatabaseConnectionProvider());
  }

  /**
   * @deprecated use {@link SqliteDatabaseDriver#SqliteDatabaseDriver(Context, String, DatabaseFilesProvider, DatabaseConnectionProvider)}
   */
  @Deprecated
  public SqliteDatabaseDriver(
      Context context,
      DatabaseFilesProvider databaseFilesProvider) {
    this(
        context,
        databaseFilesProvider,
        new DefaultDatabaseConnectionProvider());
  }

  /**
   * @param context the context
   * @param namespace label to apply to the driver when it appears in the UI
   * @param databaseFilesProvider a database file name provider
   * @param databaseConnectionProvider a database connection provider
   */
  public SqliteDatabaseDriver(
      Context context,
      DatabaseFilesProvider databaseFilesProvider,
      DatabaseConnectionProvider databaseConnectionProvider) {
    super(context);
    mDatabaseFilesProvider = databaseFilesProvider;
    mDatabaseConnectionProvider = databaseConnectionProvider;
  }

  @Override
  public List<SqliteDatabaseDescriptor> getDatabaseNames() {
    ArrayList<SqliteDatabaseDescriptor> databases = new ArrayList<>();
    List<File> potentialDatabaseFiles = mDatabaseFilesProvider.getDatabaseFiles();
    Collections.sort(potentialDatabaseFiles);
    Iterable<File> tidiedList = tidyDatabaseList(potentialDatabaseFiles);
    for (File database : tidiedList) {
      databases.add(new SqliteDatabaseDescriptor(database));
    }
    return databases;
  }

  /**
   * Attempt to smartly eliminate uninteresting shadow databases such as -journal and -uid.  Note
   * that this only removes the database if it is true that it shadows another database lacking
   * the uninteresting suffix.
   *
   * @param databaseFiles Raw list of database files.
   * @return Tidied list with shadow databases removed.
   */
  // @VisibleForTesting
  static List<File> tidyDatabaseList(List<File> databaseFiles) {
    Set<File> originalAsSet = new HashSet<File>(databaseFiles);
    List<File> tidiedList = new ArrayList<File>();
    for (File databaseFile : databaseFiles) {
      String databaseFilename = databaseFile.getPath();
      String sansSuffix = removeSuffix(databaseFilename, UNINTERESTING_FILENAME_SUFFIXES);
      if (sansSuffix.equals(databaseFilename) || !originalAsSet.contains(new File(sansSuffix))) {
        tidiedList.add(databaseFile);
      }
    }
    return tidiedList;
  }

  private static String removeSuffix(String str, String[] suffixesToRemove) {
    for (String suffix : suffixesToRemove) {
      if (str.endsWith(suffix)) {
        return str.substring(0, str.length() - suffix.length());
      }
    }
    return str;
  }

  public List<String> getTableNames(SqliteDatabaseDescriptor databaseDesc)
      throws SQLiteException {
    SQLiteDatabase database = openDatabase(databaseDesc);
    try {
      Cursor cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type IN (?, ?)",
          new String[] { "table", "view" });
      try {
        List<String> tableNames = new ArrayList<String>();
        while (cursor.moveToNext()) {
          tableNames.add(cursor.getString(0));
        }
        return tableNames;
      } finally {
        cursor.close();
      }
    } finally {
      database.close();
    }
  }

  public Database.ExecuteSQLResponse executeSQL(
      SqliteDatabaseDescriptor databaseDesc,
      String query,
      ExecuteResultHandler<Database.ExecuteSQLResponse> handler)
          throws SQLiteException {
    Util.throwIfNull(query);
    Util.throwIfNull(handler);
    SQLiteDatabase database = openDatabase(databaseDesc);
    try {
      String firstWordUpperCase = getFirstWord(query).toUpperCase();
      switch (firstWordUpperCase) {
        case "UPDATE":
        case "DELETE":
          return executeUpdateDelete(database, query, handler);
        case "INSERT":
          return executeInsert(database, query, handler);
        case "SELECT":
        case "PRAGMA":
        case "EXPLAIN":
          return executeSelect(database, query, handler);
        default:
          return executeRawQuery(database, query, handler);
      }
    } finally {
      database.close();
    }
  }

  private static String getFirstWord(String s) {
    s = s.trim();
    int firstSpace = s.indexOf(' ');
    return firstSpace >= 0 ? s.substring(0, firstSpace) : s;
  }

  @TargetApi(DatabaseConstants.MIN_API_LEVEL)
  private <T> T executeUpdateDelete(
      SQLiteDatabase database,
      String query,
      ExecuteResultHandler<T> handler) {
    SQLiteStatement statement = database.compileStatement(query);
    int count = statement.executeUpdateDelete();
    return handler.handleUpdateDelete(count);
  }

  private <T> T executeInsert(
      SQLiteDatabase database,
      String query,
      ExecuteResultHandler<T> handler) {
    SQLiteStatement statement = database.compileStatement(query);
    long count = statement.executeInsert();
    return handler.handleInsert(count);
  }

  private <T> T executeSelect(
      SQLiteDatabase database,
      String query,
      ExecuteResultHandler<T> handler) {
    Cursor cursor = database.rawQuery(query, null);
    try {
      return handler.handleSelect(cursor);
    } finally {
      cursor.close();
    }
  }

  private <T> T executeRawQuery(
      SQLiteDatabase database,
      String query,
      ExecuteResultHandler<T> handler) {
    database.execSQL(query);
    return handler.handleRawQuery();
  }

  private SQLiteDatabase openDatabase(
      SqliteDatabaseDescriptor databaseDesc)
      throws SQLiteException {
    Util.throwIfNull(databaseDesc);
    return mDatabaseConnectionProvider.openDatabase(databaseDesc.file);
  }

  static class SqliteDatabaseDescriptor implements DatabaseDescriptor {
    public final File file;

    public SqliteDatabaseDescriptor(File file) {
      this.file = file;
    }

    @Override
    public String name() {
      return file.getName();
    }
  }
}

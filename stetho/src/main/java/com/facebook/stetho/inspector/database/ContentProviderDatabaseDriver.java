/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.database;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import com.facebook.stetho.inspector.protocol.module.Database;
import com.facebook.stetho.inspector.protocol.module.DatabaseDescriptor;
import com.facebook.stetho.inspector.protocol.module.DatabaseDriver2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ContentProviderDatabaseDriver
    extends DatabaseDriver2<ContentProviderDatabaseDriver.ContentProviderDatabaseDescriptor> {

  private final static String sDatabaseName = "content-providers";

  private final ContentProviderSchema[] mContentProviderSchemas;
  private List<String> mTableNames;

  public ContentProviderDatabaseDriver(
      Context context,
      ContentProviderSchema... contentProviderSchemas) {
    super(context);
    mContentProviderSchemas = contentProviderSchemas;
  }

  @Override
  public List<ContentProviderDatabaseDescriptor> getDatabaseNames() {
    return Collections.singletonList(new ContentProviderDatabaseDescriptor());
  }

  @Override
  public List<String> getTableNames(ContentProviderDatabaseDescriptor databaseDesc) {
    if (mTableNames == null) {
      mTableNames = new ArrayList<>();
      for (ContentProviderSchema schema : mContentProviderSchemas) {
        mTableNames.add(schema.getTableName());
      }
    }
    return mTableNames;
  }

  @Override
  public Database.ExecuteSQLResponse executeSQL(
      ContentProviderDatabaseDescriptor databaseDesc,
      String query,
      ExecuteResultHandler<Database.ExecuteSQLResponse> handler) throws SQLiteException {

    // resolve table name from query
    String tableName = fetchTableName(query);

    // find the right ContentProviderSchema
    int index = mTableNames.indexOf(tableName);
    ContentProviderSchema contentProviderSchema = mContentProviderSchemas[index];

    // execute the query
    ContentResolver contentResolver = mContext.getContentResolver();
    Cursor cursor = contentResolver.query(
        contentProviderSchema.getUri(),
        contentProviderSchema.getProjection(),
        null,
        null,
        null);
    try {
      return handler.handleSelect(cursor);
    } finally {
      cursor.close();
    }
  }

  /**
   * Fetch the table name from query
   */
  private String fetchTableName(String query) {
    for (String tableName : mTableNames) {
      if (query.contains(tableName)) {
        return tableName;
      }
    }
    return "";
  }

  static class ContentProviderDatabaseDescriptor implements DatabaseDescriptor {
    public ContentProviderDatabaseDescriptor() {
    }

    @Override
    public String name() {
      // Hmm, this probably should be each unique URI or authority instead of treating all
      // content provider instances as one.
      return sDatabaseName;
    }
  }
}

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
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.protocol.module.Database;

import javax.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.List;

@ThreadSafe
public class ContentProviderDatabaseDriver extends Database.DatabaseDriver {

  private final static String sDatabaseName = "content-providers";

  private final ContentProviderSchema[] mContentProviderSchemas;
  private List<String> mTableNames;

  public ContentProviderDatabaseDriver(Context context, ContentProviderSchema... contentProviderSchemas) {
    super(context);
    mContentProviderSchemas = contentProviderSchemas;
  }

  @Override
  protected void onRegistered(JsonRpcPeer peer) {
    if (mContentProviderSchemas != null) {
      Database.DatabaseObject databaseParams = new Database.DatabaseObject();
      databaseParams.id = sDatabaseName;
      databaseParams.name = sDatabaseName;
      databaseParams.domain = mContext.getPackageName();
      databaseParams.version = "N/A";
      Database.AddDatabaseEvent eventParams = new Database.AddDatabaseEvent();
      eventParams.database = databaseParams;
      peer.invokeMethod("Database.addDatabase", eventParams, null /* callback */);
    }
  }

  @Override
  protected void onUnregistered(JsonRpcPeer peer) {

  }

  @Override
  public List<String> getDatabaseTableNames(String databaseId) {
    if (mTableNames == null) {
      mTableNames = new ArrayList<>();
      for (ContentProviderSchema schema : mContentProviderSchemas) {
        mTableNames.add(schema.getTableName());
      }
    }
    return mTableNames;
  }

  @Override
  public <T> T executeSQL(String databaseName, String query, ExecuteResultHandler<T> handler) throws SQLiteException {

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

  @Override
  public boolean contains(String databaseId) {
    return sDatabaseName.equals(databaseId);
  }

}

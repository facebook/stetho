/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.sample;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.protocol.module.Database;

import java.util.ArrayList;
import java.util.List;

/**
 * This is example of extending database domain by adding calendar provider
 */
public class CalendarProviderDatabasePeer extends Database.DatabasePeer {

  private final static String sDatabaseName = "provider-calendars";

  public CalendarProviderDatabasePeer(Context context) {
    super(context);
  }

  @Override
  protected void onRegistered(JsonRpcPeer peer) {
    Database.DatabaseObject databaseParams = new Database.DatabaseObject();
    databaseParams.id = sDatabaseName;
    databaseParams.name = sDatabaseName;
    databaseParams.domain = mContext.getPackageName();
    databaseParams.version = "N/A";
    Database.AddDatabaseEvent eventParams = new Database.AddDatabaseEvent();
    eventParams.database = databaseParams;
    peer.invokeMethod("Database.addDatabase", eventParams, null /* callback */);
  }

  @Override
  protected void onUnregistered(JsonRpcPeer peer) {

  }

  @Override
  public boolean contains(String databaseId) {
    return sDatabaseName.equals(databaseId);
  }

  @Override
  public List<String> getDatabaseTableNames(String databaseId) {
    List<String> tableNames = new ArrayList<String>();
    tableNames.add("calendars");
    tableNames.add("events");
    return tableNames;
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public <T> T executeSQL(String databaseName, String query, Database.DatabasePeer.ExecuteResultHandler<T> handler) throws SQLiteException {

    // Resolve query and make the right Cursor. Some better query parser should exist. But for now it's fine.
    Uri uri = null;
    String[] projection = null;
    if (query.contains("calendars")) {
      uri = CalendarContract.Calendars.CONTENT_URI;
      projection = new String[] {
          CalendarContract.Calendars._ID,
          CalendarContract.Calendars.NAME,
          CalendarContract.Calendars.ACCOUNT_NAME,
          CalendarContract.Calendars.IS_PRIMARY,
      };

    } else if (query.contains("events")) {
      uri = CalendarContract.Events.CONTENT_URI;
      projection = new String[] {
          CalendarContract.Events._ID,
          CalendarContract.Events.TITLE,
          CalendarContract.Events.DESCRIPTION,
          CalendarContract.Events.ACCOUNT_NAME,
          CalendarContract.Events.DTSTART,
          CalendarContract.Events.DTEND,
          CalendarContract.Events.CALENDAR_ID,
      };

    }

    ContentResolver contentResolver = mContext.getContentResolver();
    Cursor cursor = contentResolver.query(uri, projection, null, null, null);
    try {
      return handler.handleSelect(cursor);
    } finally {
      cursor.close();
    }
  }

}

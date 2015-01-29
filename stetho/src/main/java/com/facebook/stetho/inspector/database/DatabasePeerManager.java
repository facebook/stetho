// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.database;

import javax.annotation.concurrent.ThreadSafe;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.PeerRegistrationListener;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.protocol.module.Database;

@ThreadSafe
public class DatabasePeerManager extends ChromePeerManager {
  private static final String[] UNINTERESTING_FILENAME_SUFFIXES = new String[]{
      "-journal",
      "-uid"
  };

  private final Context mContext;

  public DatabasePeerManager(Context context) {
    mContext = context;
    setListener(mPeerRegistrationListener);
  }

  private void bootstrapNewPeer(JsonRpcPeer peer) {
    Iterable<String> tidiedList = tidyDatabaseList(mContext.databaseList());
    for (String databaseName : tidiedList) {
      Database.DatabaseObject databaseParams = new Database.DatabaseObject();
      databaseParams.id = databaseName;
      databaseParams.name = databaseName;
      databaseParams.domain = mContext.getPackageName();
      databaseParams.version = "N/A";
      Database.AddDatabaseEvent eventParams = new Database.AddDatabaseEvent();
      eventParams.database = databaseParams;

      peer.invokeMethod("Database.addDatabase", eventParams, null /* callback */);
    }
  }

  /**
   * Attempt to smartly eliminate uninteresting shadow databases such as -journal and -uid.  Note
   * that this only removes the database if it is true that it shadows another database lacking
   * the uninteresting suffix.
   *
   * @param databaseFilenames Raw list of database filenames.
   * @return Tidied list with shadow databases removed.
   */
  // @VisibleForTesting
  static List<String> tidyDatabaseList(String[] databaseFilenames) {
    Set<String> originalAsSet = new HashSet<String>(Arrays.asList(databaseFilenames));
    List<String> tidiedList = new ArrayList<String>();
    for (String databaseFilename : databaseFilenames) {
      String sansSuffix = removeSuffix(databaseFilename, UNINTERESTING_FILENAME_SUFFIXES);
      if (sansSuffix.equals(databaseFilename) || !originalAsSet.contains(sansSuffix)) {
        tidiedList.add(databaseFilename);
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

  public List<String> getDatabaseTableNames(String databaseName)
      throws SQLiteException {
    SQLiteDatabase database = openDatabase(databaseName);
    try {
      Cursor cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type=?",
          new String[] { "table" });
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

  public <T> T executeSQL(String databaseName, String query, ExecuteResultHandler<T> handler)
      throws SQLiteException {
    Util.throwIfNull(query);
    Util.throwIfNull(handler);
    SQLiteDatabase database = openDatabase(databaseName);
    try {
      Cursor cursor = database.rawQuery(query, null);
      try {
        return handler.handleResult(cursor);
      } finally {
        cursor.close();
      }
    } finally {
      database.close();
    }
  }

  private SQLiteDatabase openDatabase(String databaseName) throws SQLiteException {
    Util.throwIfNull(databaseName);
    File databaseFile = mContext.getDatabasePath(databaseName);

    // Execpted to throw if it cannot open the file (for example, if it doesn't exist).
    return SQLiteDatabase.openDatabase(databaseFile.getAbsolutePath(),
        null /* cursorFactory */,
        SQLiteDatabase.OPEN_READWRITE);
  }

  public interface ExecuteResultHandler<T> {
    public T handleResult(Cursor result) throws SQLiteException;
  }

  private final PeerRegistrationListener mPeerRegistrationListener =
      new PeerRegistrationListener() {
    @Override
    public void onPeerRegistered(JsonRpcPeer peer) {
      bootstrapNewPeer(peer);
    }

    @Override
    public void onPeerUnregistered(JsonRpcPeer peer) {
    }
  };
}

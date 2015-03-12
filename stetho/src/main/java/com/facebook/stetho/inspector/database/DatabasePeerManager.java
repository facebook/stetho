// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.database;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
  private final DatabaseFilesProvider mDatabaseFilesProvider;

  /**
   * Constructs the object with a {@link DatabaseFilesProvider} that supplies the database files
   * from {@link Context#databaseList()}.
   *
   * @param context the context
   * @deprecated use the other {@linkplain DatabasePeerManager#DatabasePeerManager(Context,
   * DatabaseFilesProvider) constructor} and pass in the {@linkplain DefaultDatabaseFilesProvider
   * default provider}.
   */
  @Deprecated
  public DatabasePeerManager(Context context) {
    this(context, new DefaultDatabaseFilesProvider(context));
  }

  /**
   * @param context the context
   * @param databaseFilesProvider a database file name provider
   */
  public DatabasePeerManager(Context context, DatabaseFilesProvider databaseFilesProvider) {
    mContext = context;
    mDatabaseFilesProvider = databaseFilesProvider;
    setListener(mPeerRegistrationListener);
  }

  private void bootstrapNewPeer(JsonRpcPeer peer) {
    List<File> potentialDatabaseFiles = mDatabaseFilesProvider.getDatabaseFiles();
    Iterable<File> tidiedList = tidyDatabaseList(potentialDatabaseFiles);
    for (File database : tidiedList) {
      Database.DatabaseObject databaseParams = new Database.DatabaseObject();
      databaseParams.id = database.getPath();
      databaseParams.name = database.getName();
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

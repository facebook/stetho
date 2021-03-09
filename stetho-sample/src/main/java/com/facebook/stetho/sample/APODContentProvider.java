/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.sample;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.ArrayList;

public class APODContentProvider extends ContentProvider {
  private APODSQLiteOpenHelper mOpenHelper;

  @Override
  public boolean onCreate() {
    mOpenHelper = new APODSQLiteOpenHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(
      Uri uri,
      String[] projection,
      String selection,
      String[] selectionArgs,
      String sortOrder) {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    Cursor cursor = db.query(
        APODContract.TABLE_NAME,
        projection,
        selection,
        selectionArgs,
        null /* groupBy */,
        null /* having */,
        sortOrder,
        null /* limit */);
    cursor.setNotificationUri(getContext().getContentResolver(), APODContract.CONTENT_URI);
    return cursor;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    long id = db.insert(APODContract.TABLE_NAME, null /* nullColumnHack */, values);
    notifyChange();
    return uri.buildUpon().appendEncodedPath(String.valueOf(id)).build();
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    int count = db.delete(APODContract.TABLE_NAME, selection, selectionArgs);
    notifyChange();
    return count;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    int count = db.update(APODContract.TABLE_NAME, values, selection, selectionArgs);
    notifyChange();
    return count;
  }

  @Override
  public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
      throws OperationApplicationException {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentProviderResult[] results = super.applyBatch(operations);
      db.setTransactionSuccessful();
      return results;
    } finally {
      db.endTransaction();
      notifyChange();
    }
  }

  private void notifyChange() {
    getContext().getContentResolver().notifyChange(APODContract.CONTENT_URI, null /* observer */);
  }

  private static class APODSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "apod.db";
    private static final int DB_VERSION = 2;

    public APODSQLiteOpenHelper(Context context) {
      super(context, DB_NAME, null /* factory */, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(
          "CREATE TABLE " + APODContract.TABLE_NAME + " (" +
              APODContract.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
              APODContract.Columns.TITLE + " TEXT, " +
              APODContract.Columns.DESCRIPTION_IMAGE_URL + " TEXT, " +
              APODContract.Columns.DESCRIPTION_TEXT + " TEXT, " +
              APODContract.Columns.LARGE_IMAGE_URL + " TEXT " +
              ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      drop(db);
      onCreate(db);
    }

    private void drop(SQLiteDatabase db) {
      db.execSQL("DROP TABLE " + APODContract.TABLE_NAME);
    }
  }
}

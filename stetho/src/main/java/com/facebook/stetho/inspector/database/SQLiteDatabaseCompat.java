/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.database;

import android.annotation.TargetApi;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.IntDef;

/**
 * Compatibility layer which supports opening databases with WAL and foreign key support
 * where supported.
 *
 * <p>For simplicity of implementation, all options are <em>ignored</em> prior to Honeycomb.</p>
 */
public abstract class SQLiteDatabaseCompat {
  public static final int ENABLE_WRITE_AHEAD_LOGGING = 0x1;
  public static final int ENABLE_FOREIGN_KEY_CONSTRAINTS = 0x2;
  @IntDef(
      value = { ENABLE_WRITE_AHEAD_LOGGING, ENABLE_FOREIGN_KEY_CONSTRAINTS },
      flag = true)
  public @interface SQLiteOpenOptions {}

  private static final SQLiteDatabaseCompat sInstance;

  static {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      sInstance = new JellyBeanAndBeyondImpl();
    } else {
      sInstance = new IceCreamSandwichImpl();
    }
  }

  public static SQLiteDatabaseCompat getInstance() {
    return sInstance;
  }

  public abstract int provideOpenFlags(@SQLiteOpenOptions int openOptions);
  public abstract void enableFeatures(@SQLiteOpenOptions int openOptions, SQLiteDatabase db);

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private static class JellyBeanAndBeyondImpl extends SQLiteDatabaseCompat {
    @Override
    public int provideOpenFlags(@SQLiteOpenOptions int openOptions) {
      int openFlags = 0;
      if ((openOptions & ENABLE_WRITE_AHEAD_LOGGING) != 0) {
        openFlags |= SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING;
      }
      return openFlags;
    }

    @Override
    public void enableFeatures(@SQLiteOpenOptions int openOptions, SQLiteDatabase db) {
      if ((openOptions & ENABLE_FOREIGN_KEY_CONSTRAINTS) != 0) {
        db.setForeignKeyConstraintsEnabled(true);
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class IceCreamSandwichImpl extends SQLiteDatabaseCompat {
    @Override
    public int provideOpenFlags(@SQLiteOpenOptions int openOptions) {
      return 0;
    }

    @Override
    public void enableFeatures(@SQLiteOpenOptions int openOptions, SQLiteDatabase db) {
      if ((openOptions & ENABLE_WRITE_AHEAD_LOGGING) != 0) {
        db.enableWriteAheadLogging();
      }

      if ((openOptions & ENABLE_FOREIGN_KEY_CONSTRAINTS) != 0) {
        db.execSQL("PRAGMA foreign_keys = ON");
      }
    }
  }

}

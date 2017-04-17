/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.database;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the results of {@link Context#databaseList()} for {@link SqliteDatabaseDriver}.
 */
public final class DefaultDatabaseFilesProvider implements DatabaseFilesProvider {
  private final Context mContext;

  public DefaultDatabaseFilesProvider(Context context) {
    mContext = context;
  }

  @Override
  public List<File> getDatabaseFiles() {
    List<File> databaseFiles = new ArrayList<File>();
    for (String databaseName : mContext.databaseList()) {
      databaseFiles.add(mContext.getDatabasePath(databaseName));
    }

    FilenameFilter filenameFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String filename) {
        return filename.endsWith(".sql")
            || filename.endsWith(".sqlite")
            || filename.endsWith(".db");
      }
    };

    //Add External Databases if Present
    if(isExternalAvailable()){
      final File absoluteFile = mContext.getExternalFilesDir(null).getAbsoluteFile();
      String[] externalDatabases= absoluteFile.list(filenameFilter);
      for (String db:externalDatabases){
        databaseFiles.add(new File(absoluteFile,db));
      }
    }
    return databaseFiles;
  }

  /**
   * @return true if External Storage Present
   */
  private static boolean isExternalAvailable() {
    boolean mExternalStorageAvailable = false;
    String state = Environment.getExternalStorageState();

    if (Environment.MEDIA_MOUNTED.equals(state)) {
      // We can read and write the media
      mExternalStorageAvailable = true;
    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      // We can only read the media
      mExternalStorageAvailable = true;
    } else {
      // Something else is wrong. It may be one of many other states, but all we need
      //  to know is we can neither read nor write
      mExternalStorageAvailable  = false;
    }
    return mExternalStorageAvailable;
  }
}

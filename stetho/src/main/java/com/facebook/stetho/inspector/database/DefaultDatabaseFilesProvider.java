package com.facebook.stetho.inspector.database;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the results of {@link Context#databaseList()}.
 */
public final class DefaultDatabaseFilesProvider implements DatabaseFilesProvider {

  private final Context mContext;

  public DefaultDatabaseFilesProvider(Context context) {
    mContext = context;
  }

  @Override
  public List<File> getDatabaseFiles() {
    List<File> databaseFiles = new ArrayList<File>();
    for (String filename : mContext.databaseList()) {
      databaseFiles.add(new File(filename));
    }
    return databaseFiles;
  }
}

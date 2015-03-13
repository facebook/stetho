package com.facebook.stetho.inspector.database;

import java.io.File;
import java.util.List;

/**
 * Provides a {@link List} of database files.
 */
public interface DatabaseFilesProvider {

  /**
   * Returns a {@link List} of database files.
   */
  List<File> getDatabaseFiles();

}

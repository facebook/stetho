/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

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

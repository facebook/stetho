/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.database;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class DatabasePeerManagerTest {
  @Test
  public void testTidyDatabaseList() {
    File[] databases = {
        new File("foo.db"), new File("foo.db-journal"),
        new File("bar.db"), new File("bar.db-journal"), new File( "bar.db-uid"),
        new File("baz.db"), new File("baz.db-somethingelse"),
        new File("dangling.db-journal"),
    };
    File[] expected = {
        new File( "foo.db"),
        new File("bar.db"),
        new File("baz.db"), new File("baz.db-somethingelse"),
        new File("dangling.db-journal")
    };
    List<File> tidied = SqliteDatabaseDriver.tidyDatabaseList(Arrays.asList(databases));
    assertArrayEquals(expected, tidied.toArray());
  }
}

// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.database;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class DatabasePeerManagerTest {
  @Test
  public void testTidyDatabaseList() {
     String[] databases = {
         "foo.db", "foo.db-journal",
         "bar.db", "bar.db-journal", "bar.db-uid",
         "baz.db", "baz.db-somethingelse",
         "dangling.db-journal",
     };
     String[] expected = {
         "foo.db",
         "bar.db",
         "baz.db", "baz.db-somethingelse",
         "dangling.db-journal",
     };
     List<String> tidied = DatabasePeerManager.tidyDatabaseList(databases);
     assertArrayEquals(expected, tidied.toArray());
  }
}

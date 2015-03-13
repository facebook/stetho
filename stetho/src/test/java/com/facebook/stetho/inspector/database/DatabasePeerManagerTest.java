// Copyright 2004-present Facebook. All Rights Reserved.

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
    List<File> tidied = DatabasePeerManager.tidyDatabaseList(Arrays.asList(databases));
    assertArrayEquals(expected, tidied.toArray());
  }
}

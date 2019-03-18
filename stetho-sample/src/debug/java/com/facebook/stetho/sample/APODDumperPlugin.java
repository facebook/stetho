/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.sample;

import android.content.ContentResolver;
import android.database.Cursor;
import com.facebook.stetho.dumpapp.ArgsHelper;
import com.facebook.stetho.dumpapp.DumpException;
import com.facebook.stetho.dumpapp.DumpUsageException;
import com.facebook.stetho.dumpapp.DumperContext;
import com.facebook.stetho.dumpapp.DumperPlugin;

import java.io.PrintStream;
import java.util.Iterator;

public class APODDumperPlugin implements DumperPlugin {

  private static final String NAME = "apod";

  private static final String CMD_LIST = "list";
  private static final String CMD_CLEAR = "clear";
  private static final String CMD_DELETE = "delete";
  private static final String CMD_REFRESH = "refresh";

  private final ContentResolver mContentResolver;
  private final APODRssFetcher mAPODRssFetcher;

  public APODDumperPlugin(ContentResolver contentResolver) {
    mContentResolver = contentResolver;
    mAPODRssFetcher = new APODRssFetcher(mContentResolver);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void dump(DumperContext dumpContext) throws DumpException {
    PrintStream writer = dumpContext.getStdout();
    Iterator<String> argsIter = dumpContext.getArgsAsList().iterator();

    String command = ArgsHelper.nextOptionalArg(argsIter, null);

    if (CMD_LIST.equalsIgnoreCase(command)) {
      doList(writer);
    } else if (CMD_DELETE.equalsIgnoreCase(command)) {
      doRemove(writer, argsIter);
    } else if (CMD_CLEAR.equalsIgnoreCase(command)) {
      doClear(writer);
    } else if (CMD_REFRESH.equalsIgnoreCase(command)) {
      doRefresh(writer);
    } else {
      usage(writer);
      if (command != null) {
        throw new DumpUsageException("Unknown command: " + command);
      }
    }
  }

  private void doList(PrintStream writer) {
    Cursor cursor = mContentResolver.query(
        APODContract.CONTENT_URI,
        null /* projection */,
        null /* selection */,
        null /* selectionArgs */,
        APODContract.Columns._ID);

    int count = 0;

    while (cursor.moveToNext()) {
      writer.println(String.format("Row #%d", count++));
      for (int i = 0; i < cursor.getColumnCount(); ++i) {
        writer.println(String.format("  %s: %s", cursor.getColumnName(i), cursor.getString(i)));
      }
    }

    writer.println();
  }

  private void doRemove(PrintStream writer, Iterator<String> argsIter) throws DumpUsageException {
    String rowId = ArgsHelper.nextArg(argsIter, "Expected rowId");

    delete(writer, APODContract.Columns._ID + "=?", new String[] {rowId});
  }

  private void doClear(PrintStream writer) {
    delete(writer, null, null);
  }

  private void doRefresh(PrintStream writer) {
    mAPODRssFetcher.fetchAndStore();
    writer.println("Submitted request to fetch new data");
  }

  private void delete(PrintStream writer, String where, String[] args) {
    int result = mContentResolver.delete(APODContract.CONTENT_URI, where, args);

    writer.println("Removed " + result + " rows.");
  }

  private static void usage(PrintStream writer) {
    final String cmdName = "dumpapp " + NAME;
    final String usagePrefix = "Usage: " + cmdName + " ";

    writer.println(usagePrefix + "<command> [command-options]");
    writer.print(usagePrefix + CMD_LIST);
    writer.println();
    writer.print(usagePrefix + CMD_CLEAR);
    writer.println();
    writer.print(usagePrefix + CMD_DELETE + " <rowId>");
    writer.println();
    writer.print(usagePrefix + CMD_REFRESH);
    writer.println();
  }
}

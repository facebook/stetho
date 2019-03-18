/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;

import android.text.TextUtils;

import com.facebook.stetho.dumpapp.ArgsHelper;
import com.facebook.stetho.dumpapp.DumpException;
import com.facebook.stetho.dumpapp.DumpUsageException;
import com.facebook.stetho.dumpapp.DumperContext;
import com.facebook.stetho.dumpapp.DumperPlugin;

public class HelloWorldDumperPlugin implements DumperPlugin {
  private static final String NAME = "hello";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void dump(DumperContext dumpContext) throws DumpException {
    PrintStream writer = dumpContext.getStdout();
    Iterator<String> args = dumpContext.getArgsAsList().iterator();

    String helloToWhom = ArgsHelper.nextOptionalArg(args, null);
    if (helloToWhom != null) {
      doHello(dumpContext.getStdin(), writer, helloToWhom);
    } else {
      doUsage(writer);
    }
  }

  private void doHello(InputStream in, PrintStream writer, String name) throws DumpException {
    if (TextUtils.isEmpty(name)) {
      // This will print an error to the dumpapp user and cause a non-zero exit of the
      // script.
      throw new DumpUsageException("Name is empty");
    } else if ("-".equals(name)) {
      try {
        name = new BufferedReader(new InputStreamReader(in)).readLine();
      } catch (IOException e) {
        throw new DumpException(e.toString());
      }
    }

    writer.println("Hello " + name + "!");
  }

  private void doUsage(PrintStream writer) {
    writer.println("Usage: dumpapp " + NAME + " <name>");
    writer.println();
    writer.println("If <name> is '-', the name will be read from stdin.");
  }
}

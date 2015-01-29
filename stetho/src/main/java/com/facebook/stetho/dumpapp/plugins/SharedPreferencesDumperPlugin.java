// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.dumpapp.plugins;

import javax.annotation.Nullable;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.dumpapp.DumpUsageException;
import com.facebook.stetho.dumpapp.DumperContext;
import com.facebook.stetho.dumpapp.DumperPlugin;

public class SharedPreferencesDumperPlugin implements DumperPlugin {

  private static final String XML_SUFFIX = ".xml";
  private static final String NAME = "prefs";
  private final Context mAppContext;

  public SharedPreferencesDumperPlugin(Context context) {
    mAppContext = context.getApplicationContext();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void dump(DumperContext dumpContext) throws DumpUsageException {
    PrintStream writer = dumpContext.getStdout();
    List<String> args = dumpContext.getArgsAsList();

    String commandName = args.isEmpty() ? "" : args.remove(0);

    if (commandName.equals("print")) {
      doPrint(writer, args);
    } else if (commandName.equals("write")) {
      doWrite(args);
    } else {
      doUsage(writer);
    }
  }

  /**
   * Executes command to update one value in the shared preferences
   */
  private void doWrite(List<String> args) throws DumpUsageException {
    String usagePrefix = "Usage: prefs write <path> <key> <type> <value>, where type is one of: ";
    int expectedCount = 4;
    if (args.size() != expectedCount) {
      throw new DumpUsageException(
          Type.appendNamesList(new StringBuilder(usagePrefix), ", ").toString());
    }

    int index = 0;
    String path = args.get(index++);
    String key = args.get(index++);
    String typeName = args.get(index++);
    String value = args.get(index++);
    Util.throwIfNot(index == expectedCount);

    Type type = Type.of(typeName);
    if (type == null) {
      throw new DumpUsageException(
          Type.appendNamesList(new StringBuilder(usagePrefix), ", ").toString());
    }

    SharedPreferences sharedPreferences = getSharedPreferences(path);
    SharedPreferences.Editor editor = sharedPreferences.edit();

    switch (type) {
      case BOOLEAN:
        editor.putBoolean(key, Boolean.valueOf(value));
        break;
      case INT:
        editor.putInt(key, Integer.valueOf(value));
        break;
      case LONG:
        editor.putLong(key, Long.valueOf(value));
        break;
      case FLOAT:
        editor.putFloat(key, Float.valueOf(value));
        break;
      case STRING:
        editor.putString(key, value);
        break;
    }

    editor.commit();
  }

  /**
   * Execute command to print all keys and values stored in the shared preferences which match
   * the optional given prefix
   */
  private void doPrint(PrintStream writer, List<String> args) {
    String rootPath = mAppContext.getApplicationInfo().dataDir + "/shared_prefs";
    String offsetPrefix = args.isEmpty() ? "" : args.get(0);
    String keyPrefix = (args.size() > 1) ? args.get(1) : "";

    printRecursive(writer, rootPath, "", offsetPrefix, keyPrefix);
  }

  private void printRecursive(
      PrintStream writer,
      String rootPath,
      String offsetPath,
      String pathPrefix,
      String keyPrefix) {
    File file = new File(rootPath, offsetPath);
    if (file.isFile()) {
      if (offsetPath.endsWith(XML_SUFFIX)) {
        int suffixLength = XML_SUFFIX.length();
        String prefsName = offsetPath.substring(0, offsetPath.length() - suffixLength);
        printFile(writer, prefsName, keyPrefix);
      }
    } else if (file.isDirectory()) {
      String[] children = file.list();
      if (children != null) {
        for (int i = 0; i < children.length; i++) {
          String childOffsetPath = TextUtils.isEmpty(offsetPath)
              ? children[i]
              : (offsetPath + File.separator + children[i]);
          if (childOffsetPath.startsWith(pathPrefix)) {
            printRecursive(writer, rootPath, childOffsetPath, pathPrefix, keyPrefix);
          }
        }
      }
    }
  }

  private void printFile(PrintStream writer, String prefsName, String keyPrefix) {
    writer.println(prefsName + ":");
    SharedPreferences preferences = getSharedPreferences(prefsName);
    for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
      if (entry.getKey().startsWith(keyPrefix)) {
        writer.println("  " + entry.getKey() + " = " + entry.getValue());
      }
    }
  }

  private void doUsage(PrintStream writer) {
    final String cmdName = "dumpapp " + NAME;

    String usagePrefix = "Usage: " + cmdName + " ";
    String blankPrefix = "       " + cmdName + " ";
    writer.println(usagePrefix + "<command> [command-options]");
    writer.println(usagePrefix + "print [pathPrefix [keyPrefix]]");
    writer.println(
        Type.appendNamesList(
            new StringBuilder(blankPrefix).append("write <path> <key> <"),
            "|")
            .append("> <value>"));
    writer.println();
    writer.println(cmdName + " print: Print all matching values from the shared preferences");
    writer.println();
    writer.println(cmdName + " write: Writes a value to the shared preferences");
  }

  private SharedPreferences getSharedPreferences(String name) {
    return mAppContext.getSharedPreferences(name, Context.MODE_MULTI_PROCESS);
  }

  private enum Type {
    BOOLEAN("boolean"),
    INT("int"),
    LONG("long"),
    FLOAT("float"),
    STRING("string");

    private final String name;

    private Type(String name) {
      this.name = name;
    }

    public static @Nullable Type of(String name) {
      for (Type type : values()) {
        if (type.name.equals(name)) {
          return type;
        }
      }
      return null;
    }

    public static StringBuilder appendNamesList(StringBuilder builder, String separator) {
      boolean isFirst = true;
      for (Type type : values()) {
        if (isFirst) {
          isFirst = false;
        } else {
          builder.append(separator);
        }
        builder.append(type.name);
      }
      return builder;
    }
  }
}

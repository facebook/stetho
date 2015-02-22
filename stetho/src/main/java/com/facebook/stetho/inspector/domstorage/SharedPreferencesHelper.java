package com.facebook.stetho.inspector.domstorage;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesHelper {
  private static final String PREFS_SUFFIX = ".xml";

  private SharedPreferencesHelper() {
  }

  public static List<String> getSharedPreferenceTags(Context context) {
    ArrayList<String> tags = new ArrayList<String>();

    String rootPath = context.getApplicationInfo().dataDir + "/shared_prefs";
    File root = new File(rootPath);
    if (root.exists()) {
      for (File file : root.listFiles()) {
        String fileName = file.getName();
        if (fileName.endsWith(PREFS_SUFFIX)) {
          tags.add(fileName.substring(0, fileName.length() - PREFS_SUFFIX.length()));
        }
      }
    }

    return tags;
  }

  public static String valueToString(Object value) {
    if (value != null) {
      return value.toString();
    } else {
      return null;
    }
  }
}

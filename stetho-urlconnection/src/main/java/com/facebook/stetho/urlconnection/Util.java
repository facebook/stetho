/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.urlconnection;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Util {
  public static ArrayList<Pair<String, String>> convertHeaders(Map<String, List<String>> map) {
    ArrayList<Pair<String, String>> array = new ArrayList<Pair<String, String>>();
    for (Map.Entry<String, List<String>> mapEntry : map.entrySet()) {
      for (String mapEntryValue : mapEntry.getValue()) {
        // HttpURLConnection puts a weird null entry in the header map that corresponds to
        // the HTTP response line (for instance, HTTP/1.1 200 OK).  Ignore that weirdness...
        if (mapEntry.getKey() != null) {
          array.add(Pair.create(mapEntry.getKey(), mapEntryValue));
        }
      }
    }
    return array;
  }
}

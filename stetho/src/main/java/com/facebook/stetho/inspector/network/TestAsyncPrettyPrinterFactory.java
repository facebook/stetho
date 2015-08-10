/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.network;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class TestAsyncPrettyPrinterFactory extends DownloadingAsyncPrettyPrinterFactory {
  private static final String SCHEMA_URI_PREFIX =
      "https://our.intern.facebook.com/intern/flatbuffer_schema/";

  @Override
  protected void doPrint(PrintWriter output, InputStream payload, String schema)
      throws IOException {
    output.print(schema);
  }

  @Override @Nullable
  protected MatchResult matchAndParseHeader(String headerName, String headerValue) {
    String url = SCHEMA_URI_PREFIX + "10153930580331729";
    String url1 = "http://apod.nasa.gov/apod/image/1508/ProtonArc_Williams_960.jpg";
    String url2 = "http://our.mengdilin.sb.facebook.com/intern/flatbuffer_schema/10153951711156729";
    return new MatchResult(url, PrettyPrinterDisplayType.JSON);
  }

}

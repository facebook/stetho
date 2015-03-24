/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
//
// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.dumpapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import android.content.Context;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;

/**
 * Provides a kind of CLI-over-HTTP support for raw http clients.
 * @see DumpappHandler
 */
public class RawDumpappHandler extends DumpappHandler {

  private static final String RESPONSE_HEADER_EXIT_CODE = "X-FAB-ExitCode";

  public RawDumpappHandler(Context context, Dumper dumper) {
    super(context, dumper);
  }

  @Override
  protected HttpEntity getResponseEntity(
      HttpRequest request,
      InputStream bufferedInput,
      HttpResponse response)
      throws IOException {
    ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();

    try {
      PrintStream stdout = new PrintStream(stdoutBuffer);
      try {
        ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();
        PrintStream stderr = new PrintStream(stderrBuffer);

        try {
          int exitCode = getDumper().dump(bufferedInput, stdout, stderr, getArgs(request));
          response.addHeader(RESPONSE_HEADER_EXIT_CODE, String.valueOf(exitCode));
        } finally {
          stderr.close();
          if (stderrBuffer.size() > 0) {
            System.err.write(stderrBuffer.toByteArray());
          }
        }
      } finally {
        stdout.close();
      }
    } finally {
      bufferedInput.close();
    }

    return createResponseEntity(stdoutBuffer.toByteArray());
  }

  private static HttpEntity createResponseEntity(byte[] data) {
    ByteArrayEntity entity = new ByteArrayEntity(data);
    if (isProbablyBinaryData(data)) {
      entity.setContentType(HTTP.OCTET_STREAM_TYPE);
    } else {
      entity.setContentType(HTTP.PLAIN_TEXT_TYPE);
    }
    return entity;
  }

  private static boolean isProbablyBinaryData(byte[] data) {
    for (int i = 0; i < data.length; i++) {
      byte b = data[i];
      if ((b >= 0x7f) ||
          ((b < 0x20) && (b != '\r') && (b != '\n') && (b != '\t'))) {
        return true;
      }
    }
    return false;
  }
}

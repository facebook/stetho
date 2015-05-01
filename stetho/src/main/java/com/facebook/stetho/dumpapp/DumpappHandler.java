/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.dumpapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.server.SecureHttpRequestHandler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;

/**
 * Provides a kind of CLI-over-HTTP support for the ./scripts/dumpapp tool.
 * <p>
 * This handler accepts a list of text-based arguments to a FAB endpoint and responds with
 * a stream as furnished by the Dumper implementation on the app side.  A special "exit code"
 * property is also returned that the dumpapp tool uses to pass along the exit code of the
 * script.
 */
public abstract class DumpappHandler extends SecureHttpRequestHandler {
  private static final String QUERY_PARAM_ARGV = "argv";
  private static final String RESPONSE_HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  private final Dumper mDumper;

  public DumpappHandler(Context context, Dumper dumper) {
    super(context);
    mDumper = dumper;
  }

  protected abstract HttpEntity getResponseEntity(
      HttpRequest request,
      InputStream bufferedInput,
      HttpResponse response)
      throws IOException;

  protected Dumper getDumper() {
    return mDumper;
  }


  @Override
  protected void handleSecured(
      HttpRequest request,
      HttpResponse response,
      HttpContext context) throws HttpException, IOException {
    response.setEntity(getResponseEntity(request, bufferInput(request), response));
    response.addHeader(RESPONSE_HEADER_ALLOW_ORIGIN, "*");
    response.setStatusCode(HttpStatus.SC_OK);
  }

  private static InputStream bufferInput(HttpRequest request) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    InputStream rawInput = getCallerInput(request);
    Util.copy(rawInput, buffer, new byte[256]);

    byte[] bytes = buffer.toByteArray();
    return new ByteArrayInputStream(bytes);
  }

  private static InputStream getCallerInput(HttpRequest request) throws IOException {
    if (request instanceof HttpEntityEnclosingRequest) {
      HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
      if (entity != null) {
        return entity.getContent();
      }
    }
    return new ByteArrayInputStream(new byte[] {});
  }

  protected static String[] getArgs(HttpRequest request) {
    Uri requestUri = Uri.parse(request.getRequestLine().getUri());
    List<String> argsList = requestUri.getQueryParameters(QUERY_PARAM_ARGV);
    return argsList.toArray(new String[argsList.size()]);
  }
}

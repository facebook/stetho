// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.dumpapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.protocol.HTTP;

public class StreamingDumpappHandler extends DumpappHandler {

  public StreamingDumpappHandler(Context context, Dumper dumper) {
    super(context, dumper);
  }

  @Override
  protected HttpEntity getResponseEntity(
      HttpRequest request,
      InputStream bufferedInput,
      HttpResponse response)
      throws IOException {
    DumpappHttpEntity entity = new DumpappHttpEntity(request, getDumper(), bufferedInput);
    entity.setChunked(true);
    entity.setContentType(HTTP.OCTET_STREAM_TYPE);
    return entity;
  }

  private static void writeTo(
      HttpRequest request,
      Dumper dumper,
      InputStream input,
      OutputStream output)
      throws IOException {
    StreamFramer framer = new StreamFramer(output);
    String[] args = getArgs(request);

    try {
      // We intentionally do not catch-all and write an exit code here.
      //
      // The dumper catches all expected exceptions and translates
      // them to an exit code, so the normal case is all good.
      //
      // DumpappOutputBrokenException is thrown in cases where we know
      // we are unable to write any more, including possibly while
      // writing the error code itself.
      //
      // Because other unchecked exceptions could also be thrown in
      // cases where the underlying stream is broken, and making
      // further calls on the broken stream (to write an exit code)
      // can corrupt the stream and throw still more unchecked
      // exceptions, we cannot safely write an exit code in this case.
      int exitCode = dumper.dump(input, framer.getStdout(), framer.getStderr(), args);
      framer.writeExitCode(exitCode);
    } catch (DumpappOutputBrokenException e) {
      // This exception indicates we must stop all writes to the underlying stream
      // because there was IOException.  This is otherwise harmless.
    } finally {
      framer.close();
    }
  }

  private class DumpappHttpEntity extends AbstractHttpEntity {

    private final HttpRequest mRequest;
    private final Dumper mDumper;
    private final InputStream mInput;

    DumpappHttpEntity(HttpRequest request, Dumper dumper, InputStream input) {
      mRequest = request;
      mDumper = dumper;
      mInput = input;
    }

    @Override
    public boolean isRepeatable() {
      return false;
    }

    @Override
    public long getContentLength() {
      return -1;
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStreaming() {
      return true;
    }

    @Override
    public void writeTo(OutputStream output) throws IOException {
      StreamingDumpappHandler.writeTo(mRequest, mDumper, mInput, output);
    }
  }
}

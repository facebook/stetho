// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.dumpapp;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Implements framing protocol that allows us to multiplex stdout and stderr as well
 * as exit code into a single OutputStream.
 * This is demultiplexed by fab2_frame_demux.py.
 * <p/>
 * The framing protocol involves 5-byte fixed headers, possibly followed by a variable
 * size content body.
 * The grammar is:
 * <pre>
 *   FRAME = STDOUT_FRAME | STDERR_FRAME | EXIT_FRAME
 *   STDOUT_FRAME = '1' BIG_ENDIAN_INT BODY
 *   STDERR_FRAME = '2' BIG_ENDIAN_INT BODY
 *   EXIT_FRAME = 'x' BIG_ENDIAN_INT
 *   BIG_ENDIAN_INT = (4 bytes as written by {@link DataOutputStream#writeInt})
 *   BODY = (variable-size literal BLOB)
 * </pre>
 * The BIG_ENDIAN_INT in STDOUT_FRAME / STDERR_FRAME specifies the size (in bytes) of
 * the immediately following BODY BLOB.
 * The BIG_ENDIAN_INT in EXIT_FRAME specifies the exit code.
 */
public class StreamFramer implements Closeable {

  private static final byte STDOUT_FRAME_PREFIX = '1';
  private static final byte STDERR_FRAME_PREFIX = '2';
  private static final byte EXIT_FRAME_PREFIX = 'x';

  private final PrintStream mStdout;
  private final PrintStream mStderr;
  private final DataOutputStream mMultiplexedOutputStream;

  public StreamFramer(OutputStream output) throws IOException {
    mMultiplexedOutputStream = new DataOutputStream(output);
    mStdout = new PrintStream(
        new BufferedOutputStream(
            new FramingOutputStream(mMultiplexedOutputStream, STDOUT_FRAME_PREFIX)));
    mStderr = new PrintStream(
        new FramingOutputStream(mMultiplexedOutputStream, STDERR_FRAME_PREFIX));
  }

  public PrintStream getStdout() {
    return mStdout;
  }

  public PrintStream getStderr() {
    return mStderr;
  }

  public synchronized void writeExitCode(int exitCode) throws IOException {
    mStdout.flush();
    mStderr.flush();
    writeIntFrame(EXIT_FRAME_PREFIX, exitCode);
  }

  /**
   * Closes the underlying stream without doing any more writes and without
   * the possibility of throwing {@link DumpappOutputBrokenException}.
   * <p/>
   * Note that because this can be called from a 'finally' block with an
   * {@link IOException} or {@link DumpappOutputBrokenException} pending, further
   * writes can result in unchecked exceptions, so this does not flush anything
   * from {@link #mStdout} or {@link #mStderr}.
   *
   * @see DumpappOutputBrokenException
   */
  @Override
  public synchronized void close() throws IOException {
    mMultiplexedOutputStream.close();
  }

  private void writeIntFrame(byte type, int intParameter) throws IOException {
    mMultiplexedOutputStream.write(type);
    mMultiplexedOutputStream.writeInt(intParameter);
  }

  private class FramingOutputStream extends FilterOutputStream {

    private final byte mPrefix;

    FramingOutputStream(DataOutputStream innerStream, byte prefix) {
      super(innerStream);
      mPrefix = prefix;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
      if (length > 0) {
        try {
          synchronized (StreamFramer.this) {
            writeIntFrame(mPrefix, length);
            mMultiplexedOutputStream.write(buffer, offset, length);
            mMultiplexedOutputStream.flush();
          }
        } catch (IOException e) {
          // I/O error here can indicate the pipe is broken, so we need to prevent any
          // further writes.
          throw new DumpappOutputBrokenException(e);
        }
      }
    }

    @Override
    public void write(int oneByte) throws IOException {
      byte[] buffer = new byte[] { (byte)oneByte };
      write(buffer, 0, buffer.length);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
      write(buffer, 0, buffer.length);
    }
  }
}

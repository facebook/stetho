package com.facebook.stetho.inspector.network;

import java.io.IOException;

/**
 * Custom hook to intercept read events delivered by
 * {@link NetworkEventReporter#interpretResponseStream}.
 */
public interface ResponseHandler {
  /**
   * Signal that data has been read from the response stream.  Note that some HTTP implementations
   * (like okhttp) will automatically decompress for you so you must hook in at a lower level
   * than the default response handler to properly account for raw, compressed response bytes.
   *
   * @param numBytes Bytes read from the network stack's stream as established by
   *     {@link NetworkEventReporter#interpretResponseStream}.
   */
  public void onRead(int numBytes);

  /**
   * Signals that EOF has been reached reading the response stream from the network
   * stack.
   */
  public void onEOF();

  /**
   * Signals that an error occurred while reading the response stream.
   */
  public void onError(IOException e);
}

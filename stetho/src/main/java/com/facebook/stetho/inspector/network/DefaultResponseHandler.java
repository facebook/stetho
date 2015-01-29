package com.facebook.stetho.inspector.network;

import java.io.IOException;

/**
 * Simple interceptor that delegates response read events to {@link NetworkEventReporter}.
 */
public class DefaultResponseHandler implements ResponseHandler {
  private final NetworkEventReporter mEventReporter;
  private final String mRequestId;

  private int mBytesRead = 0;

  public DefaultResponseHandler(NetworkEventReporter eventReporter, String requestId) {
    mEventReporter = eventReporter;
    mRequestId = requestId;
  }

  public void onRead(int numBytes) {
    // Simply count the bytes received so we can report it at the end.  Note that it is
    // an expected part of the protocol to deliver this data in discrete chunks but
    // unfortunately our implementation is so inefficient that it creates noticable
    // lag in the application.  We could either optimize message delivery or just buffer; we
    // choose buffering :)
    mBytesRead += numBytes;
  }

  public void onEOF() {
    reportDataReceived();
    mEventReporter.responseReadFinished(mRequestId);
  }

  public void onError(IOException e) {
    reportDataReceived();
    mEventReporter.responseReadFailed(mRequestId, e.toString());
  }

  private void reportDataReceived() {
    mEventReporter.dataReceived(mRequestId, mBytesRead, mBytesRead);
  }
}

package com.facebook.stetho.inspector.network;

public class SimpleTextInspectorWebSocketFrame
    implements NetworkEventReporter.InspectorWebSocketFrame {
  private final String mRequestId;
  private final String mPayload;

  public SimpleTextInspectorWebSocketFrame(String requestId, String payload) {
    mRequestId = requestId;
    mPayload = payload;
  }

  @Override
  public String requestId() {
    return mRequestId;
  }

  @Override
  public int opcode() {
    return OPCODE_TEXT;
  }

  @Override
  public boolean mask() {
    return false;
  }

  @Override
  public String payloadData() {
    return mPayload;
  }
}

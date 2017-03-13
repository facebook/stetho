package com.facebook.stetho.inspector.network;

import java.io.UnsupportedEncodingException;

public class SimpleBinaryInspectorWebSocketFrame
    implements NetworkEventReporter.InspectorWebSocketFrame {
  private final String mRequestId;
  private final byte[] mPayload;

  public SimpleBinaryInspectorWebSocketFrame(String requestId, byte[] payload) {
    mRequestId = requestId;
    mPayload = payload;
  }

  @Override
  public String requestId() {
    return mRequestId;
  }

  @Override
  public int opcode() {
    return OPCODE_BINARY;
  }

  @Override
  public boolean mask() {
    return false;
  }

  @Override
  public String payloadData() {
    try {
      // LOL, yes this is really how Chrome does it too...
      return new String(mPayload, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}

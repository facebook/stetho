// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.websocket;

/**
 * Alternative to JSR-356's Session class but with a less insane J2EE-style API.
 */
public interface SimpleSession {
  public void sendText(String payload);
  public void sendBinary(byte[] payload);

  /**
   * Request that the session be closed.
   *
   * @param closeReason Close reason, as per RFC6455
   * @param reasonPhrase Possibly arbitrary close reason phrase.
   */
  public void close(int closeReason, String reasonPhrase);

  public boolean isOpen();
}

// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.websocket;

import java.io.IOException;

interface WriteCallback {
  public void onFailure(IOException e);
  public void onSuccess();
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.server;

import com.facebook.stetho.common.LogUtil;

import java.io.IOException;
import java.util.ArrayList;

public class ServerManager {
  private static final String THREAD_PREFIX = "StethoListener";
  private final LocalSocketServer mServer;

  private volatile boolean mStarted;

  public ServerManager(LocalSocketServer server) {
    mServer = server;
  }

  public void start() {
    if (mStarted) {
      throw new IllegalStateException("Already started");
    }
    mStarted = true;
    startServer(mServer);
  }

  private void startServer(final LocalSocketServer server) {
    Thread listener = new Thread(THREAD_PREFIX + "-" + server.getName()) {
      @Override
      public void run() {
        try {
          server.run();
        } catch (IOException e) {
          LogUtil.e(e, "Could not start Stetho server: %s", server.getName());
        }
      }
    };
    listener.start();
  }
}

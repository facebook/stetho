/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.os.Handler;
import android.os.Looper;

public final class HandlerUtil {
  private HandlerUtil() {
  }

  public static boolean postAndWait(Handler handler, final Runnable r) {
    if (Looper.myLooper() == handler.getLooper()) {
      r.run();
      return true;
    }

    WaitableRunnable wrapper = new WaitableRunnable(r);
    if (!handler.post(wrapper)) {
      return false;
    }

    wrapper.join();
    return true;
  }

  private static final class WaitableRunnable implements Runnable {
    private final Runnable mRunnable;
    private boolean mIsDone;

    public WaitableRunnable(Runnable runnable) {
      mRunnable = runnable;
    }

    @Override
    public void run() {
      try {
        mRunnable.run();
      } finally {
        synchronized (this) {
          mIsDone = true;
          notifyAll();
        }
      }
    }

    public void join() {
      synchronized (this) {
        while (!mIsDone) {
          try {
            wait();
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }
}

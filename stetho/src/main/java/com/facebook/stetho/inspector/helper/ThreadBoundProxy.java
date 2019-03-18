/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.helper;

import android.os.Handler;
import com.facebook.stetho.common.ThreadBound;
import com.facebook.stetho.common.UncheckedCallable;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.common.android.HandlerUtil;

/**
 * This class is for those cases when a class' threading
 * policy is determined by one of its member variables.
 */
public abstract class ThreadBoundProxy implements ThreadBound {
  private final ThreadBound mEnforcer;

  public ThreadBoundProxy(ThreadBound enforcer) {
    mEnforcer = Util.throwIfNull(enforcer);
  }

  @Override
  public final boolean checkThreadAccess() {
    return mEnforcer.checkThreadAccess();
  }

  @Override
  public final void verifyThreadAccess() {
    mEnforcer.verifyThreadAccess();
  }

  @Override
  public final <V> V postAndWait(UncheckedCallable<V> c) {
    return mEnforcer.postAndWait(c);
  }

  @Override
  public final void postAndWait(Runnable r) {
    mEnforcer.postAndWait(r);
  }

  @Override
  public final void postDelayed(Runnable r, long delayMillis) {
    mEnforcer.postDelayed(r, delayMillis);
  }

  @Override
  public final void removeCallbacks(Runnable r) {
    mEnforcer.removeCallbacks(r);
  }
}

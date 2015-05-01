/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.ThreadBound;
import com.facebook.stetho.common.UncheckedCallable;
import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;

public abstract class Descriptor implements NodeDescriptor {
  private Host mHost;

  protected Descriptor() {
  }

  void initialize(Host host) {
    Util.throwIfNull(host);
    Util.throwIfNotNull(mHost);
    mHost = host;
  }

  boolean isInitialized() {
    return mHost != null;
  }

  protected final Host getHost() {
    return mHost;
  }

  @Override
  public final boolean checkThreadAccess() {
    return getHost().checkThreadAccess();
  }

  @Override
  public final void verifyThreadAccess() {
    getHost().verifyThreadAccess();
  }

  @Override
  public final <V> V postAndWait(UncheckedCallable<V> c) {
    return getHost().postAndWait(c);
  }

  @Override
  public final void postAndWait(Runnable r) {
    getHost().postAndWait(r);
  }

  public interface Host extends ThreadBound {
    @Nullable
    public Descriptor getDescriptor(@Nullable Object element);

    public void onAttributeModified(
        Object element,
        String name,
        String value);

    public void onAttributeRemoved(
        Object element,
        String name);

    public void onChildInserted(
        Object parentElement,
        @Nullable Object previousElement,
        Object childElement);

    public void onChildRemoved(
        Object parentElement,
        Object childElement);
  }
}

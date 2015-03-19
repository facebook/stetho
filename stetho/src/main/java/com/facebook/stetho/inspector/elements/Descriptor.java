// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements;

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

  public interface Host {
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

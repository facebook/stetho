// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements;

import javax.annotation.Nullable;

public abstract class Descriptor implements NodeDescriptor {

  private Listener mListener;

  protected Descriptor() {
  }

  void setListener(Listener listener) {
    mListener = listener;
  }

  protected final Listener getListener() {
    return mListener;
  }

  public interface Listener {
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

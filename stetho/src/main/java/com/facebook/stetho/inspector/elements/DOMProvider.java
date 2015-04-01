// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements;

import javax.annotation.Nullable;

public interface DOMProvider {
  public void setListener(Listener listener);

  public void dispose();

  @Nullable
  public Object getRootElement();

  @Nullable
  public NodeDescriptor getNodeDescriptor(@Nullable Object element);

  public void highlightElement(Object element, int color);

  public void hideHighlight();

  public void setInspectModeEnabled(boolean enabled);

  public static interface Factory {
    DOMProvider create();
  }

  public static interface Listener {
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

    public void onInspectRequested(
        Object element);
  }
}

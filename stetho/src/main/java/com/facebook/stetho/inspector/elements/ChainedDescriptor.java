// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;

public abstract class ChainedDescriptor<E> extends Descriptor {
  private Descriptor mSuper;

  // This is used by DescriptorMap to hook us up to whatever handles E's super class.
  // This method is idempotent in the sense that once you call it with a specific
  // reference you must either 1) never call it again, or 2) call it again with that
  // same reference.
  final void setSuper(Descriptor superDescriptor) {
    Util.throwIfNull(superDescriptor);

    if (superDescriptor != mSuper) {
      if (mSuper != null) {
        throw new IllegalStateException();
      }
      mSuper = superDescriptor;
    }
  }

  public final Descriptor getSuper() {
    return mSuper;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void hook(Object element) {
    mSuper.hook(element);
    onHook((E)element);
  }

  protected void onHook(E element) {
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void unhook(Object element) {
    onUnhook((E)element);
    mSuper.unhook(element);
  }

  protected void onUnhook(E element) {
  }

  @Override
  @SuppressWarnings("unchecked")
  public final NodeType getNodeType(Object element) {
    return onGetNodeType((E)element);
  }

  protected NodeType onGetNodeType(E element) {
    return mSuper.getNodeType(element);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final String getNodeName(Object element) {
    return onGetNodeName((E)element);
  }

  protected String onGetNodeName(E element) {
    return mSuper.getNodeName(element);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final String getLocalName(Object element) {
    return onGetLocalName((E)element);
  }

  protected String onGetLocalName(E element) {
    return mSuper.getLocalName(element);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final String getNodeValue(Object element) {
    return onGetNodeValue((E)element);
  }

  @Nullable
  public String onGetNodeValue(E element) {
    return mSuper.getNodeValue(element);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final int getChildCount(Object element) {
    int superCount = mSuper.getChildCount(element);
    int derivedCount = onGetChildCount((E) element);
    return superCount + derivedCount;
  }

  protected int onGetChildCount(E element) {
    return 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final Object getChildAt(Object element, int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException();
    }

    int superCount = mSuper.getChildCount(element);
    if (index < superCount) {
      return mSuper.getChildAt(element, index);
    }

    int thisCount = onGetChildCount((E)element);
    int thisIndex = index - superCount;
    if (thisIndex < 0 || thisIndex >= thisCount) {
      throw new IndexOutOfBoundsException();
    }

    return onGetChildAt((E)element, thisIndex);
  }

  protected Object onGetChildAt(E element, int index) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void copyAttributes(Object element, AttributeAccumulator attributes) {
    mSuper.copyAttributes(element, attributes);
    onCopyAttributes((E)element, attributes);
  }

  protected void onCopyAttributes(E element, AttributeAccumulator attributes) {
  }
}
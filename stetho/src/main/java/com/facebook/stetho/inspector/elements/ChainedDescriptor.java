// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.ThreadBound;
import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;

/**
 * This class implements {@link Descriptor} in a way that is specially understood by
 * {@link DescriptorMap}. When implemented and registered for a class {@link E}, an instance of this
 * class will automatically be chained to the {@link Descriptor} which is registered for {@link E}'s
 * next super class. If {@link E}'s immediate super class doesn't have a descriptor registered for
 * it, but the super-super class does, then that will be used. This allows you to implement
 * {@link Descriptor} for any class without having to worry about describing anything about the
 * super class, and without having to know which {@link Descriptor} is registered for that super
 * class.
 *
 * <p>For example, let's say you wanted to write a {@link Descriptor} for
 * {@link android.widget.ListView}. You can certainly derive from {@link Descriptor} and write
 * code to describe everything exposed by {@link android.widget.ListView},
 * {@link android.view.ViewGroup}, {@link android.view.View}, and {@link java.lang.Object}. Or you
 * can implement descriptors for each of these classes and create a parallel inheritance
 * hierarchy (e.g. your descriptor for {@link android.view.ViewGroup} would derive from your
 * descriptor for {@link android.view.View}, at which point you have to worry about things like
 * aggregating the results of {@link Descriptor#getChildAt(Object, int)} and so on. In both cases
 * you'll also have a lot of messy unchecked casts to worry about.</p>
 *
 * <p>Or, you can derive from {@link ChainedDescriptor} and then only worry about describing what
 * {@link android.widget.ListView} adds to or changes from {@link android.view.ViewGroup}.
 * Aggregation and casting are handled for you. You're also protected from the fragility that
 * would occur if a {@link Descriptor} was later implemented for something in-between
 * {@link android.view.ViewGroup} and {@link android.widget.ListView}, such as
 * {@link android.widget.AbsListView}. Your descriptor will automatically chain to and benefit from
 * the descriptor registered for the nearest super class.</p>
 *
 * <p>This class also implements the thread safety enforcement policy prescribed by
 * {@link ThreadBound}. Namely, that {@link #verifyThreadAccess()}} needs to be called in the
 * prologue for every method. Your derived class SHOULD NOT call {@link #verifyThreadAccess()}} in
 * any of its on___() methods.
 * </p>
 *
 * <p>(NOTE: As an optimization, {@link #verifyThreadAccess()} is not actually called in the
 * prologue for every method. Instead, we rely on {@link DOMProvider#getNodeDescriptor(Object)}
 * calling it in order to get most of our enforcement coverage. We still call
 * {@link #verifyThreadAccess()} in a few important methods such as {@link #hook(Object)} and
 * {@link #unhook(Object)} (anything that writes or is potentially really dangerous if misused).
 *
 * @param <E> the class that this descriptor will be describing for {@link DOMProvider} and
 * {@link com.facebook.stetho.inspector.protocol.module.DOM}
 */
public abstract class ChainedDescriptor<E> extends Descriptor {

  private Descriptor mSuper;

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
    verifyThreadAccess();
    mSuper.hook(element);
    onHook((E)element);
  }

  protected void onHook(E element) {
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void unhook(Object element) {
    verifyThreadAccess();
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
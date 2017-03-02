/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ThreadBound;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.protocol.module.DOM;

import javax.annotation.Nullable;

/**
 * This class derives from {@link Descriptor} and provides a canonical implementation of
 * {@link ChainedDescriptor}.<p/>
 *
 * This class implements the thread safety enforcement policy prescribed by
 * {@link ThreadBound}. Namely, that {@link #verifyThreadAccess()}} needs to be called in the
 * prologue for every method. Your derived class SHOULD NOT call {@link #verifyThreadAccess()}} in
 * any of its on___() methods.<p/>
 *
 * (NOTE: As an optimization, {@link #verifyThreadAccess()} is not actually called in the
 * prologue for every method. Instead, we rely on {@link DocumentProvider#getNodeDescriptor(Object)}
 * calling it in order to get most of our enforcement coverage. We still call
 * {@link #verifyThreadAccess()} in a few important methods such as {@link #hook(Object)} and
 * {@link #unhook(Object)} (anything that writes or is potentially really dangerous if misused).<p/>
 *
 * @param <E> the class that this descriptor will be describing for {@link DocumentProvider},
 * {@link Document}, and ultimately {@link DOM}.
 */
public abstract class AbstractChainedDescriptor<E>
    extends Descriptor<E> implements ChainedDescriptor<E> {

  private Descriptor<? super E> mSuper;

  @Override
  public void setSuper(Descriptor<? super E> superDescriptor) {
    Util.throwIfNull(superDescriptor);

    if (superDescriptor != mSuper) {
      if (mSuper != null) {
        throw new IllegalStateException();
      }
      mSuper = superDescriptor;
    }
  }

  final Descriptor<? super E> getSuper() {
    return mSuper;
  }

  @Override
  public final void hook(E element) {
    verifyThreadAccess();
    mSuper.hook(element);
    onHook(element);
  }

  protected void onHook(E element) {
  }

  @Override
  public final void unhook(E element) {
    verifyThreadAccess();
    onUnhook(element);
    mSuper.unhook(element);
  }

  protected void onUnhook(E element) {
  }

  @Override
  public final NodeType getNodeType(E element) {
    return onGetNodeType(element);
  }

  protected NodeType onGetNodeType(E element) {
    return mSuper.getNodeType(element);
  }

  @Override
  public final String getNodeName(E element) {
    return onGetNodeName(element);
  }

  protected String onGetNodeName(E element) {
    return mSuper.getNodeName(element);
  }

  @Override
  public final String getLocalName(E element) {
    return onGetLocalName(element);
  }

  protected String onGetLocalName(E element) {
    return mSuper.getLocalName(element);
  }

  @Override
  public final String getNodeValue(E element) {
    return onGetNodeValue(element);
  }

  @Nullable
  public String onGetNodeValue(E element) {
    return mSuper.getNodeValue(element);
  }

  @Override
  public final void getChildren(E element, Accumulator<Object> children) {
    mSuper.getChildren(element, children);
    onGetChildren(element, children);
  }

  protected void onGetChildren(E element, Accumulator<Object> children) {
  }

  @Override
  public final void getAttributes(E element, AttributeAccumulator attributes) {
    mSuper.getAttributes(element, attributes);
    onGetAttributes(element, attributes);
  }

  protected void onGetAttributes(E element, AttributeAccumulator attributes) {
  }

  @Override
  public final void setAttributesAsText(E element, String text) {
    onSetAttributesAsText(element, text);
  }

  protected void onSetAttributesAsText(E element, String text) {
    mSuper.setAttributesAsText(element, text);
  }

  @Override
  public final void getStyleRuleNames(E element, StyleRuleNameAccumulator accumulator) {
    mSuper.getStyleRuleNames(element, accumulator);
    onGetStyleRuleNames(element, accumulator);
  }

  protected void onGetStyleRuleNames(E element, StyleRuleNameAccumulator accumulator) {
  }

  @Override
  public final void getStyles(E element, String ruleName, StyleAccumulator accumulator) {
    mSuper.getStyles(element, ruleName, accumulator);
    onGetStyles(element, ruleName, accumulator);
  }

  protected void onGetStyles(E element, String ruleName, StyleAccumulator accumulator) {
  }

  @Override
  public final void setStyle(E element, String ruleName, String name, String value) {
    mSuper.setStyle(element, ruleName, name, value);
    onSetStyle(element, ruleName, name, value);
  }

  protected void onSetStyle(E element, String ruleName, String name, String value) {
  }

  @Override
  public void getComputedStyles(E element, ComputedStyleAccumulator accumulator) {
    mSuper.getComputedStyles(element, accumulator);
    onGetComputedStyles(element, accumulator);
  }

  protected void onGetComputedStyles(E element, ComputedStyleAccumulator accumulator) {
  }
}

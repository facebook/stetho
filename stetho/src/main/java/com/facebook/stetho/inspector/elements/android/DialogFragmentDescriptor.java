/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Dialog;
import android.graphics.Rect;
import android.view.View;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.common.android.DialogFragmentAccessor;
import com.facebook.stetho.common.android.FragmentCompat;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.DescriptorMap;
import com.facebook.stetho.inspector.elements.NodeID;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.elements.StyleAccumulator;

import javax.annotation.Nullable;

final class DialogFragmentDescriptor
    extends Descriptor<Object>
    implements ChainedDescriptor<Object>, HighlightableDescriptor<Object> {
  private final DialogFragmentAccessor mAccessor;
  private Descriptor<? super Object> mSuper;

  public static DescriptorMap register(DescriptorMap map) {
    maybeRegister(map, FragmentCompat.getSupportLibInstance());
    maybeRegister(map, FragmentCompat.getFrameworkInstance());
    return map;
  }

  private static void maybeRegister(DescriptorMap map, @Nullable FragmentCompat compat) {
    if (compat != null) {
      Class<?> dialogFragmentClass = compat.getDialogFragmentClass();
      LogUtil.d("Adding support for %s", dialogFragmentClass);
      map.registerDescriptor(dialogFragmentClass, new DialogFragmentDescriptor(compat));
    }
  }

  private DialogFragmentDescriptor(FragmentCompat compat) {
    mAccessor = compat.forDialogFragment();
  }

  @Override
  public void setSuper(Descriptor<? super Object> superDescriptor) {
    Util.throwIfNull(superDescriptor);

    if (superDescriptor != mSuper) {
      if (mSuper != null) {
        throw new IllegalStateException();
      }
      mSuper = superDescriptor;
    }
  }

  @Override
  public void hook(Object element) {
    mSuper.hook(element);
  }

  @Override
  public void unhook(Object element) {
    mSuper.unhook(element);
  }

  @Override
  public NodeType getNodeType(Object element) {
    return mSuper.getNodeType(element);
  }

  @Override
  public String getNodeName(Object element) {
    return mSuper.getNodeName(element);
  }

  @Override
  public String getLocalName(Object element) {
    return mSuper.getLocalName(element);
  }

  @Nullable
  @Override
  public String getNodeValue(Object element) {
    return mSuper.getNodeValue(element);
  }

  @Override
  public void getChildren(Object element, Accumulator<Object> children) {
    /**
     * We do NOT want the childrenIDs from our super-{@link Descriptor}, which is probably
     * {@link FragmentDescriptor}. We only want to emit the {@link Dialog}, not the {@link View}.
     * Therefore, we don't call mSuper.getChildren(), and this is the reason why we don't derive
     * from {@link AbstractChainedDescriptor} (it doesn't allow a non-chained implementation of
     * {@link Descriptor#getChildren(Object, Accumulator)}).
     */
    children.store(mAccessor.getDialog(element));
  }

  @Override
  public void getAttributes(Object element, AttributeAccumulator attributes) {
    mSuper.getAttributes(element, attributes);
  }

  @Override
  public void setAttributesAsText(Object element, String text) {
    mSuper.setAttributesAsText(element, text);
  }

  @Nullable
  @Override
  public View getViewAndBoundsForHighlighting(Object element, Rect bounds) {
    final Descriptor.Host host = getHost();
    if (host instanceof AndroidDescriptorHost) {
      Dialog dialog = mAccessor.getDialog(element);
      return ((AndroidDescriptorHost) host).getHighlightingView(dialog, bounds);
    }

    return null;
  }

  @Override
  public void getStyles(Object element, StyleAccumulator styles) {
  }

  @Override
  public void getAccessibilityStyles(Object element, StyleAccumulator accumulator) {
  }

  @Override
  public NodeID getNodeID(Object element) {
    return mSuper.getNodeID(element);
  }
}

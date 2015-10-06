/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Activity;
import android.app.Dialog;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ReflectionUtil;
import com.facebook.stetho.common.StringUtil;
import com.facebook.stetho.common.android.FragmentActivityAccessor;
import com.facebook.stetho.common.android.FragmentCompat;
import com.facebook.stetho.common.android.FragmentManagerAccessor;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.Descriptor;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.List;

final class ActivityDescriptor
    extends AbstractChainedDescriptor<Activity> implements HighlightableDescriptor {
  @Override
  protected String onGetNodeName(Activity element) {
    String className = element.getClass().getName();
    return StringUtil.removePrefix(className, "android.app.");
  }

  @Override
  protected void onGetChildren(Activity element, Accumulator<Object> children) {
    getDialogFragments(FragmentCompat.getSupportLibInstance(), element, children);
    getDialogFragments(FragmentCompat.getFrameworkInstance(), element, children);

    getDialogs(element, children);

    Window window = element.getWindow();
    if (window != null) {
      children.store(window);
    }
  }

  private void getDialogs(Activity activity, Accumulator<Object> children) {
    // TODO: optimize reflection use

    // We don't need to worry about double-emitting any Dialogs that are hosted by DialogFragments
    // because they are not included in Activity.mManagedDialogs.

    Field fieldMManagedDialogs = ReflectionUtil.tryGetDeclaredField(
        Activity.class,
        "mManagedDialogs");
    if (fieldMManagedDialogs == null) {
      return;
    }
    fieldMManagedDialogs.setAccessible(true);

    SparseArray managedDialogs;
    try {
      managedDialogs = (SparseArray) fieldMManagedDialogs.get(activity);
    } catch (IllegalAccessException | IllegalArgumentException ex) {
      return;
    }

    if (managedDialogs == null) {
      return;
    }

    Field fieldMDialog = null;
    for (int i = 0, size = managedDialogs.size(); i < size; ++i) {
      Object managedDialog = managedDialogs.valueAt(i);

      if (fieldMDialog == null) {
        fieldMDialog = ReflectionUtil.tryGetDeclaredField(managedDialog.getClass(), "mDialog");
      }
      if (fieldMDialog == null) {
        return;
      }
      fieldMDialog.setAccessible(true);

      Dialog dialog;
      try {
        dialog = (Dialog) fieldMDialog.get(managedDialog);
      } catch (IllegalAccessException | IllegalArgumentException ex) {
        return;
      }

      children.store(dialog);
    }
  }

  @Override
  public View getViewForHighlighting(Object element) {
    final Descriptor.Host host = getHost();
    if (host instanceof AndroidDescriptorHost) {
      Activity activity = (Activity)element;
      Window window = activity.getWindow();
      return ((AndroidDescriptorHost) host).getHighlightingView(window);
    }

    return null;
  }

  private static void getDialogFragments(
      @Nullable FragmentCompat compat,
      Activity activity,
      Accumulator<Object> accumulator) {
    if (compat == null || !compat.getFragmentActivityClass().isInstance(activity)) {
      return;
    }

    FragmentActivityAccessor activityAccessor = compat.forFragmentActivity();
    Object fragmentManager = activityAccessor.getFragmentManager(activity);
    if (fragmentManager == null) {
      return;
    }

    FragmentManagerAccessor fragmentManagerAccessor = compat.forFragmentManager();
    List<Object> addedFragments = fragmentManagerAccessor.getAddedFragments(fragmentManager);
    if (addedFragments == null) {
      return;
    }

    for (int i = 0, N = addedFragments.size(); i < N; ++i) {
      final Object fragment = addedFragments.get(i);
      if (compat.getDialogFragmentClass().isInstance(fragment)) {
        accumulator.store(fragment);
      }
    }
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.common.android;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.Spinner;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * This class provides utility methods for determining certain accessibility properties of
 * {@link View}s and {@link AccessibilityNodeInfoCompat}s. It is porting some of the checks from
 * {@link com.googlecode.eyesfree.utils.AccessibilityNodeInfoUtils}, but has stripped many features
 * which are unnecessary here.
 */
public final class AccessibilityUtil {
  private AccessibilityUtil() {
  }

  /**
   * Returns whether the specified node has text or a content description.
   *
   * @param node The node to check.
   * @return {@code true} if the node has text.
   */
  public static boolean hasText(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }

    return !TextUtils.isEmpty(node.getText()) || !TextUtils.isEmpty(node.getContentDescription());
  }

  /**
   * Returns whether the supplied {@link View} and {@link AccessibilityNodeInfoCompat} would
   * produce spoken feedback if it were accessibility focused.  NOTE: not all speaking nodes are
   * focusable.
   *
   * @param view The {@link View} to evaluate
   * @param node The {@link AccessibilityNodeInfoCompat} to evaluate
   * @return {@code true} if it meets the criterion for producing spoken feedback
   */
  public static boolean isSpeakingNode(
      @Nullable AccessibilityNodeInfoCompat node,
      @Nullable View view) {
    if (node == null || view == null) {
      return false;
    }

    if (!node.isVisibleToUser()) {
      return false;
    }

    int important = ViewCompat.getImportantForAccessibility(view);
    if (important == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS ||
        (important == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO &&
            node.getChildCount() <= 0)) {
      return false;
    }

    return node.isCheckable() || hasText(node) || hasNonActionableSpeakingDescendants(node, view);
  }

  /**
   * Determines if the supplied {@link View} and {@link AccessibilityNodeInfoCompat} has any
   * children which are not independently accessibility focusable and also have a spoken
   * description.
   * <p>
   * NOTE: Accessibility services will include these children's descriptions in the closest
   * focusable ancestor.
   *
   * @param view The {@link View} to evaluate
   * @param node The {@link AccessibilityNodeInfoCompat} to evaluate
   * @return {@code true} if it has any non-actionable speaking descendants within its subtree
   */
  public static boolean hasNonActionableSpeakingDescendants(
      @Nullable AccessibilityNodeInfoCompat node,
      @Nullable View view) {

    if (node == null || view == null || !(view instanceof ViewGroup)) {
      return false;
    }

    ViewGroup viewGroup = (ViewGroup) view;
    for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
      View childView = viewGroup.getChildAt(i);

      if (childView == null) {
        continue;
      }

      AccessibilityNodeInfoCompat childNode = AccessibilityNodeInfoCompat.obtain();
      try {
        ViewCompat.onInitializeAccessibilityNodeInfo(childView, childNode);

        if (isAccessibilityFocusable(childNode, childView)) {
          continue;
        }

        if (isSpeakingNode(childNode, childView)) {
          return true;
        }
      } finally {
        childNode.recycle();
      }
    }

    return false;
  }

  /**
   * Determines if the provided {@link View} and {@link AccessibilityNodeInfoCompat} meet the
   * criteria for gaining accessibility focus.
   *
   * @param view The {@link View} to evaluate
   * @param node The {@link AccessibilityNodeInfoCompat} to evaluate
   * @return {@code true} if it is possible to gain accessibility focus
   */
  public static boolean isAccessibilityFocusable(
      @Nullable AccessibilityNodeInfoCompat node,
      @Nullable View view) {
    if (node == null || view == null) {
      return false;
    }

    // Never focus invisible nodes.
    if (!node.isVisibleToUser()) {
      return false;
    }

    // Always focus "actionable" nodes.
    if (isActionableForAccessibility(node)) {
      return true;
    }

    // only focus top-level list items with non-actionable speaking children.
    return isTopLevelScrollItem(node, view) && isSpeakingNode(node, view);
  }

  /**
   * Determines whether the provided {@link View} and {@link AccessibilityNodeInfoCompat} is a
   * top-level item in a scrollable container.
   *
   * @param view The {@link View} to evaluate
   * @param node The {@link AccessibilityNodeInfoCompat} to evaluate
   * @return {@code true} if it is a top-level item in a scrollable container.
   */
  public static boolean isTopLevelScrollItem(
      @Nullable AccessibilityNodeInfoCompat node,
      @Nullable View view) {
    if (node == null || view == null) {
      return false;
    }

    View parent = (View) ViewCompat.getParentForAccessibility(view);
    if (parent == null) {
      return false;
    }

    if (node.isScrollable()) {
      return true;
    }

    List actionList = node.getActionList();
    if (actionList.contains(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) ||
        actionList.contains(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)) {
      return true;
    }

    // AdapterView, ScrollView, and HorizontalScrollView are focusable
    // containers, but Spinner is a special case.
    if (parent instanceof Spinner) {
      return false;
    }

    return
        parent instanceof AdapterView ||
            parent instanceof ScrollView ||
            parent instanceof HorizontalScrollView;
  }

  /**
   * Returns whether a node is actionable. That is, the node supports one of
   * {@link AccessibilityNodeInfoCompat#isClickable()},
   * {@link AccessibilityNodeInfoCompat#isFocusable()}, or
   * {@link AccessibilityNodeInfoCompat#isLongClickable()}.
   *
   * @param node The {@link AccessibilityNodeInfoCompat} to evaluate
   * @return {@code true} if node is actionable.
   */
  public static boolean isActionableForAccessibility(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }

    if (node.isClickable() || node.isLongClickable() || node.isFocusable()) {
      return true;
    }

    List actionList = node.getActionList();
    return
        actionList.contains(AccessibilityNodeInfoCompat.ACTION_CLICK) ||
            actionList.contains(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK) ||
            actionList.contains(AccessibilityNodeInfoCompat.ACTION_FOCUS);
  }

  /**
   * Determines if any of the provided {@link View}'s and {@link AccessibilityNodeInfoCompat}'s
   * ancestors can receive accessibility focus
   *
   * @param view The {@link View} to evaluate
   * @param node The {@link AccessibilityNodeInfoCompat} to evaluate
   * @return {@code true} if an ancestor of may receive accessibility focus
   */
  public static boolean hasFocusableAncestor(
      @Nullable AccessibilityNodeInfoCompat node,
      @Nullable View view) {
    if (node == null || view == null) {
      return false;
    }

    ViewParent parentView = ViewCompat.getParentForAccessibility(view);
    if (!(parentView instanceof View)) {
      return false;
    }

    AccessibilityNodeInfoCompat parentNode = AccessibilityNodeInfoCompat.obtain();
    try {
      ViewCompat.onInitializeAccessibilityNodeInfo((View) parentView, parentNode);
      if (parentNode == null) {
        return false;
      }

      if (isAccessibilityFocusable(parentNode, (View) parentView)) {
        return true;
      }

      if (hasFocusableAncestor(parentNode, (View) parentView)) {
        return true;
      }
    } finally {
      parentNode.recycle();
    }
    return false;
  }
}

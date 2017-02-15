/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

import com.facebook.stetho.common.android.AccessibilityUtil;

import java.util.ArrayList;
import java.util.List;

public final class AccessibilityNodeInfoWrapper {

  public AccessibilityNodeInfoWrapper() {
  }

  public static boolean getIgnored(AccessibilityNodeInfoCompat node, View view) {
    int important = ViewCompat.getImportantForAccessibility(view);
    if (important == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO ||
        important == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
      return true;
    }

    // Go all the way up the tree to make sure no parent has hidden its descendants
    ViewParent parent = view.getParent();
    while (parent instanceof View) {
      if (ViewCompat.getImportantForAccessibility((View) parent)
          == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
        return true;
      }
      parent = parent.getParent();
    }

    if (!node.isVisibleToUser()) {
      return true;
    }

    if (AccessibilityUtil.isAccessibilityFocusable(node, view)) {
      if (node.getChildCount() <= 0) {
        // Leaves that are accessibility focusable are never ignored, even if they don't have a
        // speakable description
        return false;
      } else if (AccessibilityUtil.isSpeakingNode(node, view)) {
        // Node is focusable and has something to speak
        return false;
      }

      // Node is focusable and has nothing to speak
      return true;
    }

    // If this nodeID has no focusable ancestors, but it still has text,
    // then it should receive focus from navigation and be read aloud.
    if (!AccessibilityUtil.hasFocusableAncestor(node, view) && AccessibilityUtil.hasText(node)) {
      return false;
    }

    return true;
  }

  public static String getIgnoredReasons(AccessibilityNodeInfoCompat node, View view) {
    int important = ViewCompat.getImportantForAccessibility(view);

    if (important == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO) {
      return "View has importantForAccessibility set to 'NO'.";
    }

    if (important == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
      return "View has importantForAccessibility set to 'NO_HIDE_DESCENDANTS'.";
    }

    ViewParent parent = view.getParent();
    while (parent instanceof View) {
      if (ViewCompat.getImportantForAccessibility((View) parent)
              == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
        return "An ancestor View has importantForAccessibility set to 'NO_HIDE_DESCENDANTS'.";
      }
      parent = parent.getParent();
    }

    if (!node.isVisibleToUser()) {
      return "View is not visible.";
    }

    if (AccessibilityUtil.isAccessibilityFocusable(node, view)) {
      return "View is actionable, but has no description.";
    }

    if (AccessibilityUtil.hasText(node)) {
      return "View is not actionable, and an ancestor View has co-opted its description.";
    }

    return "View is not actionable and has no description.";
  }

  @Nullable
  public static String getFocusableReasons(AccessibilityNodeInfoCompat node, View view) {
    boolean hasText = AccessibilityUtil.hasText(node);
    boolean isCheckable = node.isCheckable();
    boolean hasNonActionableSpeakingDescendants =
        AccessibilityUtil.hasNonActionableSpeakingDescendants(node, view);

    if (AccessibilityUtil.isActionableForAccessibility(node)) {
      if (node.getChildCount() <= 0) {
        return "View is actionable and has no childrenIDs.";
      } else if (hasText) {
        return "View is actionable and has a description.";
      } else if (isCheckable) {
        return "View is actionable and checkable.";
      } else if (hasNonActionableSpeakingDescendants) {
        return "View is actionable and has non-actionable descendants with descriptions.";
      }
    }

    if (AccessibilityUtil.isTopLevelScrollItem(node, view)) {
      if (hasText) {
        return "View is a direct child of a scrollable container and has a description.";
      } else if (isCheckable) {
        return "View is a direct child of a scrollable container and is checkable.";
      } else if (hasNonActionableSpeakingDescendants) {
        return
            "View is a direct child of a scrollable container and has non-actionable " +
            "descendants with descriptions.";
      }
    }

    if (hasText) {
      return "View has a description and is not actionable, but has no actionable ancestor.";
    }

    return null;
  }

  @Nullable
  public static String getActions(AccessibilityNodeInfoCompat node) {
    final StringBuilder actionLabels = new StringBuilder();
    final String separator = ", ";

    for (AccessibilityActionCompat action : node.getActionList()) {
      if (actionLabels.length() > 0) {
        actionLabels.append(separator);
      }
      switch (action.getId()) {
        case AccessibilityNodeInfoCompat.ACTION_FOCUS:
          actionLabels.append("focus");
          break;
        case AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS:
          actionLabels.append("clear-focus");
          break;
        case AccessibilityNodeInfoCompat.ACTION_SELECT:
          actionLabels.append("select");
          break;
        case AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION:
          actionLabels.append("clear-selection");
          break;
        case AccessibilityNodeInfoCompat.ACTION_CLICK:
          actionLabels.append("click");
          break;
        case AccessibilityNodeInfoCompat.ACTION_LONG_CLICK:
          actionLabels.append("long-click");
          break;
        case AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS:
          actionLabels.append("accessibility-focus");
          break;
        case AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
          actionLabels.append("clear-accessibility-focus");
          break;
        case AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY:
          actionLabels.append("next-at-movement-granularity");
          break;
        case AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY:
          actionLabels.append("previous-at-movement-granularity");
          break;
        case AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT:
          actionLabels.append("next-html-nodeID");
          break;
        case AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT:
          actionLabels.append("previous-html-nodeID");
          break;
        case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
          actionLabels.append("scroll-forward");
          break;
        case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
          actionLabels.append("scroll-backward");
          break;
        case AccessibilityNodeInfoCompat.ACTION_CUT:
          actionLabels.append("cut");
          break;
        case AccessibilityNodeInfoCompat.ACTION_COPY:
          actionLabels.append("copy");
          break;
        case AccessibilityNodeInfoCompat.ACTION_PASTE:
          actionLabels.append("paste");
          break;
        case AccessibilityNodeInfoCompat.ACTION_SET_SELECTION:
          actionLabels.append("set-selection");
          break;
        default:
          CharSequence label = action.getLabel();
          if (label != null) {
            actionLabels.append(label);
          } else {
            actionLabels.append("unknown");
          }
          break;
      }
    }

    return actionLabels.length() > 0 ? actionLabels.toString() : null;
  }

  @Nullable
  public static CharSequence getDescription(AccessibilityNodeInfoCompat node, View view) {
    CharSequence contentDescription = node.getContentDescription();
    CharSequence nodeText = node.getText();

    boolean hasNodeText = !TextUtils.isEmpty(nodeText);
    boolean isEditText = view instanceof EditText;

    // EditText's prioritize their own text content over a contentDescription
    if (!TextUtils.isEmpty(contentDescription) && (!isEditText || !hasNodeText)) {
      return contentDescription;
    }

    if (hasNodeText) {
      return nodeText;
    }

    // If there are child views and no contentDescription the text of all non-focusable childrenIDs,
    // comma separated, becomes the description.
    if (view instanceof ViewGroup) {
      final StringBuilder concatChildDescription = new StringBuilder();
      final String separator = ", ";
      ViewGroup viewGroup = (ViewGroup) view;

      for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
        final View child = viewGroup.getChildAt(i);

        AccessibilityNodeInfoCompat childNodeInfo = AccessibilityNodeInfoCompat.obtain();
        ViewCompat.onInitializeAccessibilityNodeInfo(child, childNodeInfo);

        CharSequence childNodeDescription = null;
        if (AccessibilityUtil.isSpeakingNode(childNodeInfo, child) &&
            !AccessibilityUtil.isAccessibilityFocusable(childNodeInfo, child)) {
          childNodeDescription = getDescription(childNodeInfo, child);
        }

        if (!TextUtils.isEmpty(childNodeDescription)) {
          if (concatChildDescription.length() > 0) {
            concatChildDescription.append(separator);
          }
          concatChildDescription.append(childNodeDescription);
        }
        childNodeInfo.recycle();
      }

      return concatChildDescription.length() > 0 ? concatChildDescription.toString() : null;
    }

    return null;
  }
}

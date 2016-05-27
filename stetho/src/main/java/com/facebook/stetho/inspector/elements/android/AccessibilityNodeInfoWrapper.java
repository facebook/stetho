/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;
import android.view.ViewParent;

public final class AccessibilityNodeInfoWrapper {

  public AccessibilityNodeInfoWrapper() {
  }

  public static boolean getIgnored(View view, AccessibilityNodeInfoCompat nodeInfo) {
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

    // AccessibilityNodeInfo is actionable
    if (nodeInfo.isClickable() || nodeInfo.isLongClickable() || nodeInfo.isFocusable()) {
      return false;
    }

    // AccessibilityNodeInfo has text set (commonly set from TextViews and Switches).
    CharSequence nodeText = nodeInfo.getText();
    if (nodeText != null && nodeText.length() > 0) {
      return false;
    }

    // AccessibilityNodeInfo has a content description
    CharSequence contentDescription = nodeInfo.getContentDescription();
    if (contentDescription != null && contentDescription.length() > 0) {
      return false;
    }

    // View has an AccessibilityNodeProvider
    if (ViewCompat.getAccessibilityNodeProvider(view) != null) {
      return false;
    }

    // View has an AccessibilityDelegate
    if (ViewCompat.hasAccessibilityDelegate(view)){
      return false;
    }

    return true;
  }
}

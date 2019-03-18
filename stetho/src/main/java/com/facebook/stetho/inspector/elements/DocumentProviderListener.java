/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements;

public interface DocumentProviderListener {
  void onPossiblyChanged();

  void onAttributeModified(
      Object element,
      String name,
      String value);

  void onAttributeRemoved(
      Object element,
      String name);

  void onInspectRequested(
      Object element);
}

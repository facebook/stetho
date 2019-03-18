/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements.android;

import com.facebook.stetho.inspector.elements.Descriptor;

import javax.annotation.Nullable;

interface AndroidDescriptorHost extends Descriptor.Host {
  @Nullable
  HighlightableDescriptor getHighlightableDescriptor(@Nullable Object element);
}

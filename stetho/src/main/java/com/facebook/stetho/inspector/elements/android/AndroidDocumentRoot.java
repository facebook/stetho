/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Application;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.NodeType;

// For the root, we use 1 object for both element and descriptor.

final class AndroidDocumentRoot extends AbstractChainedDescriptor<AndroidDocumentRoot> {
  private final Application mApplication;

  public AndroidDocumentRoot(Application application) {
    mApplication = Util.throwIfNull(application);
  }

  @Override
  protected NodeType onGetNodeType(AndroidDocumentRoot element) {
    return NodeType.DOCUMENT_NODE;
  }

  @Override
  protected String onGetNodeName(AndroidDocumentRoot element) {
    return "root";
  }

  @Override
  protected void onGetChildren(AndroidDocumentRoot element, Accumulator<Object> children) {
    children.store(mApplication);
  }
}

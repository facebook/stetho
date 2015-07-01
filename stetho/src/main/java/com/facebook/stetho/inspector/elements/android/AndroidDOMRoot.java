/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.elements.android;

import android.app.Application;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.NodeType;

// For the root, we use 1 object for both element and descriptor.

final class AndroidDOMRoot extends AbstractChainedDescriptor<AndroidDOMRoot> {
  private final Application mApplication;

  public AndroidDOMRoot(Application application) {
    mApplication = Util.throwIfNull(application);
  }

  @Override
  protected NodeType onGetNodeType(AndroidDOMRoot element) {
    return NodeType.DOCUMENT_NODE;
  }

  @Override
  protected String onGetNodeName(AndroidDOMRoot element) {
    return "root";
  }

  @Override
  protected void onGetChildren(AndroidDOMRoot element, Accumulator<Object> children) {
    children.store(mApplication);
  }
}

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.network;

import com.facebook.stetho.inspector.protocol.module.Page;

public enum PrettyPrinterDisplayType {
  JSON(Page.ResourceType.XHR),
  HTML(Page.ResourceType.DOCUMENT),
  TEXT(Page.ResourceType.DOCUMENT);

  private final Page.ResourceType mResourceType;

  private PrettyPrinterDisplayType(Page.ResourceType resourceType) {
    mResourceType = resourceType;
  }

  /**
   * Converts PrettyPrinterDisplayType values to the appropriate
   *  {@link Page.ResourceType} values that Stetho understands
   */
  public Page.ResourceType getResourceType() {
    return mResourceType;
  }
}

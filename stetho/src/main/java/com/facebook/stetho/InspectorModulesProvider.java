// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho;

import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;

public interface InspectorModulesProvider {
  public Iterable<ChromeDevtoolsDomain> get();
}

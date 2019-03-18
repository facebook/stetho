/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho;

import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;

public interface InspectorModulesProvider {
  Iterable<ChromeDevtoolsDomain> get();
}

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.urlconnection;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Narrow alternative to Apache's HttpEntity which makes it easier to repeat the body
 * so that Stetho can intercept it.  This simplification makes it possible to avoid
 * also using an intercepted stream for POST bodies as we do with responses.
 */
public interface SimpleRequestEntity {
  void writeTo(OutputStream out) throws IOException;
}

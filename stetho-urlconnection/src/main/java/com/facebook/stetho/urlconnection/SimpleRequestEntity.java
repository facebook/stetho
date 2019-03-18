/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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

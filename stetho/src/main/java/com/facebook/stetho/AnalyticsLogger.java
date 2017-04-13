/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Use to track usage of stetho. By default stetho is initialized with a null logger and will not
 * log anything. However if you would like to track usage of stetho you may provide an
 * implementation of this interface.
 */
public interface AnalyticsLogger {

  /**
   * @param event The name of the event being tracked
   * @param data Any additional data to attach to this event
   */
  void track(String event, @Nullable Map<String, String> data);
}

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.sample;

import android.net.Uri;
import android.provider.BaseColumns;

public interface APODContract {
  String AUTHORITY = "com.facebook.stetho.sample.apod";
  Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  String TABLE_NAME = "rss_items";

  interface Columns extends BaseColumns {
    String TITLE = "title";
    String DESCRIPTION_TEXT = "description_text";
    String DESCRIPTION_IMAGE_URL = "description_image_url";
    String LARGE_IMAGE_URL = "large_image_url";
  }
}

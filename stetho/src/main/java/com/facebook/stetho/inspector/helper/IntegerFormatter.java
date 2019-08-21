/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.helper;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.ViewDebug;

import androidx.annotation.Nullable;

public class IntegerFormatter {
  private static IntegerFormatter cachedFormatter;

  public static IntegerFormatter getInstance() {
    if (cachedFormatter == null) {
      synchronized (IntegerFormatter.class) {
        if (cachedFormatter == null) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cachedFormatter = new IntegerFormatterWithHex();
          } else {
            cachedFormatter = new IntegerFormatter();
          }
        }
      }
    }

    return cachedFormatter;
  }

  private IntegerFormatter() {
  }

  public String format(Integer integer, @Nullable ViewDebug.ExportedProperty annotation) {
    return String.valueOf(integer);
  }

  private static class IntegerFormatterWithHex extends IntegerFormatter {
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String format(Integer integer, @Nullable ViewDebug.ExportedProperty annotation) {
      if (annotation != null && annotation.formatToHexString()) {
        return "0x" + Integer.toHexString(integer);
      }

      return super.format(integer, annotation);
    }
  }
}

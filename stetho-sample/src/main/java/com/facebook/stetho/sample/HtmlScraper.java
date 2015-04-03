/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.sample;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

import javax.annotation.Nullable;
import java.util.List;

public class HtmlScraper {
  /**
   * Scrapes an HTML page for &lt;img&gt; tags.
   *
   * @return Scraped plain text
   */
  public static String parseWithImageTags(
      String htmlText,
      @Nullable String originUrl,
      List<String> outImageUrls) {
    ExtractImageGetter imageGetter = new ExtractImageGetter(originUrl, outImageUrls);
    String strippedText = Html.fromHtml(
        htmlText,
        imageGetter,
        null /* tagHandler */)
        .toString();

    return strippedText.trim();
  }

  private static class ExtractImageGetter implements Html.ImageGetter {
    @Nullable private final String mOriginUrl;
    private final List<String> mSources;

    public ExtractImageGetter(@Nullable String originUrl, List<String> outSources) {
      mOriginUrl = originUrl;
      mSources = outSources;
    }

    @Override
    public Drawable getDrawable(String source) {
      if (mOriginUrl != null && TextUtils.isEmpty(Uri.parse(source).getScheme())) {
        StringBuilder newSource = new StringBuilder();
        newSource.append(mOriginUrl);
        if (!mOriginUrl.endsWith("/") && !source.startsWith("/")) {
          newSource.append("/");
        }
        newSource.append(source);
        source = newSource.toString();
      }
      mSources.add(source);

      // Dummy drawable.
      return new ColorDrawable(Color.TRANSPARENT);
    }

    public List<String> getSources() {
      return mSources;
    }
  }
}

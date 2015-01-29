package com.facebook.stetho.sample;

import android.net.Uri;
import android.provider.BaseColumns;

public interface APODContract {
  public static final String AUTHORITY = "com.facebook.stetho.sample.apod";
  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  public static final String TABLE_NAME = "rss_items";

  public interface Columns extends BaseColumns {
    public static final String TITLE = "title";
    public static final String DESCRIPTION_TEXT = "description_text";
    public static final String DESCRIPTION_IMAGE_URL = "description_image_url";
  }
}

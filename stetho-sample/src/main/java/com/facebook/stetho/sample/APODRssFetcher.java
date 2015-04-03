/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.sample;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import android.util.Xml;
import com.facebook.stetho.common.Utf8Charset;
import com.facebook.stetho.common.Util;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class APODRssFetcher {
  private static final String TAG = "APODRssFetcher";
  private static final String APOD_RSS_URL = "http://apod.nasa.gov/apod.rss";

  private final ContentResolver mContentResolver;

  public APODRssFetcher(ContentResolver contentResolver) {
    mContentResolver = contentResolver;
  }

  public void fetchAndStore() {
    Networker.HttpRequest request = Networker.HttpRequest.newBuilder()
        .friendlyName("APOD RSS")
        .method(Networker.HttpMethod.GET)
        .url(APOD_RSS_URL)
        .build();
    Networker.get().submit(request, mStoreRssResponse);
  }

  private final Networker.Callback mStoreRssResponse = new Networker.Callback() {
    @Override
    public void onResponse(Networker.HttpResponse result) {
      if (result.statusCode == 200) {
        try {
          List<RssItem> rssItems = parseRss(result.body);
          List<ApodItem> apodItems = decorateRssItemsWithLinkImages(rssItems);
          store(apodItems);
        } catch (XmlPullParserException e) {
          Log.e(TAG, "Parse error", e);
        } catch (OperationApplicationException e) {
          Log.e(TAG, "Database write error", e);
        } catch (RemoteException e) {
          // Not recoverable, our process or the system_server must be dying...
          throw new RuntimeException(e);
        } catch (IOException e) {
          // Reading from a byte[] shouldn't cause this...
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public void onFailure(IOException e) {
      // Show in Stetho :)
    }

    private List<RssItem> parseRss(byte[] body) throws IOException, XmlPullParserException {
      XmlPullParser parser = Xml.newPullParser();
      parser.setInput(new ByteArrayInputStream(body), "UTF-8");
      List<RssItem> items = new RssParser(parser).parse();
      Log.d(TAG, "Fetched " + items.size() + " items");

      return items;
    }

    public List<ApodItem> decorateRssItemsWithLinkImages(List<RssItem> rssItems) {
      ArrayList<ApodItem> apodItems = new ArrayList<>(rssItems.size());
      final CountDownLatch fetchLinkLatch = new CountDownLatch(rssItems.size());
      for (RssItem rssItem : rssItems) {
        final ApodItem apodItem = new ApodItem();
        apodItem.rssItem = rssItem;
        fetchLinkPage(rssItem.link, new PageScrapedCallback() {
          @Override
          public void onPageScraped(@Nullable List<String> imageUrls) {
            apodItem.largeImageUrl = imageUrls != null && !imageUrls.isEmpty()
                ? imageUrls.get(0)
                : null;
            fetchLinkLatch.countDown();
          }
        });
        apodItems.add(apodItem);
      }

      // Wait for all link fetches to complete, despite running them in parallel...
      Util.awaitUninterruptibly(fetchLinkLatch);

      return apodItems;
    }

    private void store(List<ApodItem> items) throws RemoteException, OperationApplicationException {
      ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
      operations.add(
          ContentProviderOperation.newDelete(APODContract.CONTENT_URI)
              .build());
      for (ApodItem item : items) {
        Log.d(TAG, "Add item: " + item.rssItem.title);
        operations.add(
            ContentProviderOperation.newInsert(APODContract.CONTENT_URI)
                .withValues(convertItemToValues(item))
                .build());
      }

      mContentResolver.applyBatch(APODContract.AUTHORITY, operations);
    }

    private ContentValues convertItemToValues(ApodItem item) {
      ContentValues values = new ContentValues();
      values.put(APODContract.Columns.TITLE, item.rssItem.title);

      ArrayList<String> imageUrls = new ArrayList<>();
      String strippedText = HtmlScraper.parseWithImageTags(
          item.rssItem.description,
          null /* origin */,
          imageUrls);

      // Hack to remove some strange non-printing character at the start...
      strippedText = strippedText.substring(1).trim();
      String imageUrl = !imageUrls.isEmpty() ? imageUrls.get(0) : null;

      values.put(APODContract.Columns.DESCRIPTION_IMAGE_URL, imageUrl);
      values.put(APODContract.Columns.DESCRIPTION_TEXT, strippedText);
      values.put(APODContract.Columns.LARGE_IMAGE_URL, item.largeImageUrl);

      return values;
    }
  };

  private void fetchLinkPage(String linkUrl, PageScrapedCallback callback) {
    String originUrl = getOriginUri(Uri.parse(linkUrl)).toString();
    Networker.HttpRequest request = Networker.HttpRequest.newBuilder()
        .friendlyName("fetchLinkPage")
        .method(Networker.HttpMethod.GET)
        .url(linkUrl)
        .build();
    Networker.get().submit(request, new PageScrapeNetworkCallback(originUrl, callback));
  }

  private static Uri getOriginUri(Uri uri) {
    Uri.Builder b = uri.buildUpon();
    b.encodedPath(null);

    List<String> segments = uri.getPathSegments();
    for (int i = 0; i < segments.size() - 1; i++) {
      b.appendEncodedPath(segments.get(i));
    }

    return b.build();
  }

  private static class PageScrapeNetworkCallback implements Networker.Callback {
    @Nullable private final String mOrigin;
    private final PageScrapedCallback mDelegate;

    public PageScrapeNetworkCallback(@Nullable String origin, PageScrapedCallback delegate) {
      mOrigin = origin;
      mDelegate = delegate;
    }

    @Override
    public void onResponse(Networker.HttpResponse result) {
      ArrayList<String> imageUrls = new ArrayList<>();
      String htmlText = Utf8Charset.decodeUTF8(result.body);
      HtmlScraper.parseWithImageTags(htmlText, mOrigin, imageUrls);
      mDelegate.onPageScraped(imageUrls);
    }

    @Override
    public void onFailure(IOException e) {
      mDelegate.onPageScraped(null /* imageUrls */);
    }
  }

  private static class RssParser {
    private final XmlPullParser mParser;

    public RssParser(XmlPullParser parser) {
      mParser = parser;
    }

    public List<RssItem> parse() throws IOException, XmlPullParserException {
      ArrayList<RssItem> items = new ArrayList<RssItem>();

      mParser.nextTag();
      mParser.require(XmlPullParser.START_TAG, null, "rss");
      mParser.nextTag();
      mParser.require(XmlPullParser.START_TAG, null, "channel");

      while (mParser.next() != XmlPullParser.END_TAG) {
        if (mParser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }

        String name = mParser.getName();
        if (name.equals("item")) {
          items.add(readItem());
        } else {
          skip();
        }
      }

      return items;
    }

    private RssItem readItem() throws XmlPullParserException, IOException {
      mParser.require(XmlPullParser.START_TAG, null, "item");
      RssItem item = new RssItem();
      while (mParser.next() != XmlPullParser.END_TAG) {
        if (mParser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        String name = mParser.getName();
        if (name.equals("title")) {
          item.title = readTextFromTag("title");
        } else if (name.equals("description")) {
          item.description = readTextFromTag("description");
        } else if (name.equals("link")) {
          item.link = readTextFromTag("link");
        } else {
          skip();
        }
      }
      return item;
    }

    private String readTextFromTag(String tagName) throws IOException, XmlPullParserException {
      mParser.require(XmlPullParser.START_TAG, null, tagName);
      String text = readText();
      mParser.require(XmlPullParser.END_TAG, null, tagName);
      return text;
    }

    private String readText() throws IOException, XmlPullParserException {
      String result = "";
      if (mParser.next() == XmlPullParser.TEXT) {
        result = mParser.getText();
        mParser.nextTag();
      }
      return result;
    }

    private void skip() throws IOException, XmlPullParserException {
      if (mParser.getEventType() != XmlPullParser.START_TAG) {
        throw new IllegalStateException();
      }
      int depth = 1;
      while (depth != 0) {
        switch (mParser.next()) {
          case XmlPullParser.END_TAG:
            depth--;
            break;
          case XmlPullParser.START_TAG:
            depth++;
            break;
        }
      }
    }
  }

  private static class RssItem {
    public String title;
    public String description;
    public String link;
  }

  private static class ApodItem {
    public RssItem rssItem;
    @Nullable public String largeImageUrl;
  }

  private interface PageScrapedCallback {
    /**
     * @param imageUrls Image URLs that were scraped or null if the page could not be fetched or
     *     parsed.
     */
    public void onPageScraped(@Nullable List<String> imageUrls);
  }
}

package com.facebook.stetho.sample;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
        .method(Networker.HttpMethod.GET, null /* body */)
        .url(APOD_RSS_URL)
        .build();
    Networker.get().submit(request, mStoreRssResponse);
  }

  private final Networker.Callback mStoreRssResponse = new Networker.Callback() {
    @Override
    public void onResponse(Networker.HttpResponse result) {
      if (result.statusCode == 200) {
        try {
          parseAndStore(result.body);
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

    private void parseAndStore(byte[] body)
        throws
            XmlPullParserException,
            RemoteException,
            OperationApplicationException,
            IOException {
      XmlPullParser parser = Xml.newPullParser();
      parser.setInput(new ByteArrayInputStream(body), "UTF-8");
      List<RssItem> items = new RssParser(parser).parse();
      Log.d(TAG, "Fetched " + items.size() + " items");

      ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
      operations.add(
          ContentProviderOperation.newDelete(APODContract.CONTENT_URI)
              .build());
      for (RssItem item : items) {
        Log.d(TAG, "Add item: " + item.title);
        operations.add(
            ContentProviderOperation.newInsert(APODContract.CONTENT_URI)
                .withValues(convertItemToValues(item))
                .build());
      }

      mContentResolver.applyBatch(APODContract.AUTHORITY, operations);
    }

    private ContentValues convertItemToValues(RssItem item) {
      ContentValues values = new ContentValues();
      values.put(APODContract.Columns.TITLE, item.title);

      ExtractImageGetter imageGetter = new ExtractImageGetter();
      String strippedText = Html.fromHtml(
          item.description,
          imageGetter,
          null /* tagHandler */)
          .toString();

      // Hack to remove some strange non-printing character at the start...
      strippedText = strippedText.substring(1).trim();
      List<String> imageUrls = imageGetter.getSources();
      String imageUrl = !imageUrls.isEmpty() ? imageUrls.get(0) : null;

      values.put(APODContract.Columns.DESCRIPTION_IMAGE_URL, imageUrl);
      values.put(APODContract.Columns.DESCRIPTION_TEXT, strippedText);

      return values;
    }
  };

  private static class ExtractImageGetter implements Html.ImageGetter {
    private final ArrayList<String> mSources = new ArrayList<String>();

    @Override
    public Drawable getDrawable(String source) {
      mSources.add(source);

      // Dummy drawable.
      return new ColorDrawable(Color.TRANSPARENT);
    }

    public List<String> getSources() {
      return mSources;
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
  }
}

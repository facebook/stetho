/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.sample;

import java.io.IOException;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Simple demonstration of fetching and caching a specific RSS feed showing the
 * "Astronomy Picture of the Day" feed from NASA.  This demonstrates both the database access
 * and network inspection features of Stetho.
 */
public class APODActivity extends ListActivity {
  private static final int LOADER_APOD_POSTS = 1;

  private static final String TAG = "APODActivity";

  private APODPostsAdapter mAdapter;

  public static void show(Context context) {
    context.startActivity(new Intent(context, APODActivity.class));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getLoaderManager().initLoader(LOADER_APOD_POSTS, new Bundle(), mLoaderCallback);

    new APODRssFetcher(getContentResolver()).fetchAndStore();

    mAdapter = new APODPostsAdapter(this);
    setListAdapter(mAdapter);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    getLoaderManager().destroyLoader(LOADER_APOD_POSTS);
  }

  private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback =
      new LoaderManager.LoaderCallbacks<Cursor>() {
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      switch (id) {
        case LOADER_APOD_POSTS:
          return APODPostsQuery.createCursorLoader(APODActivity.this);
        default:
          throw new IllegalArgumentException("id=" + id);
      }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
      mAdapter.changeCursor(null);
    }
  };

  private class APODPostsAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;

    public APODPostsAdapter(Context context) {
      super(context, null /* cursor */, false /* autoRequery */);
      mInflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      View view = mInflater.inflate(R.layout.apod_list_item, parent, false);
      view.setTag(new ViewHolder(view));
      return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
      ViewHolder holder = (ViewHolder)view.getTag();
      int bindPosition = cursor.getPosition();
      holder.position = bindPosition;

      final String imageUrl = cursor.getString(APODPostsQuery.LARGE_IMAGE_URL_INDEX);
      holder.image.setImageDrawable(null);
      fetchImage(imageUrl, bindPosition, holder);

      cursor.copyStringToBuffer(APODPostsQuery.TITLE_INDEX, holder.titleBuffer);

      setTextWithBuffer(holder.title, holder.titleBuffer);

      cursor.copyStringToBuffer(APODPostsQuery.DESCRIPTION_TEXT_INDEX, holder.descriptionBuffer);
      setTextWithBuffer(holder.description, holder.descriptionBuffer);
    }

    // Really crude image handling.  Please don't do this in a real app :)
    private void fetchImage(
        final String imageUrl,
        final int bindPosition,
        final ViewHolder holder) {
      Networker.HttpRequest imageRequest = Networker.HttpRequest.newBuilder()
          .method(Networker.HttpMethod.GET)
          .url(imageUrl)
          .build();
      Networker.get().submit(imageRequest, new Networker.Callback() {
        @Override
        public void onResponse(Networker.HttpResponse result) {
          if (bindPosition == holder.position) {
            Log.d(TAG, "Got " + imageUrl + ": " + result.statusCode + ", " + result.body.length);
            if (result.statusCode == 200) {
              final Bitmap bitmap =
                  BitmapFactory.decodeByteArray(result.body, 0, result.body.length);
              APODActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  holder.image.setImageDrawable(new BitmapDrawable(bitmap));
                }
              });
            }
          }
        }

        @Override
        public void onFailure(IOException e) {
          // Let Stetho demonstrate the errors :)
        }
      });
    }
  }

  private static class ViewHolder {
    public final ImageView image;
    public final TextView title;
    public final CharArrayBuffer titleBuffer = new CharArrayBuffer(32);
    public final TextView description;
    public final CharArrayBuffer descriptionBuffer = new CharArrayBuffer(64);

    int position;

    public ViewHolder(View v) {
      image = (ImageView)v.findViewById(R.id.image);
      title = (TextView)v.findViewById(R.id.title);
      description = (TextView)v.findViewById(R.id.description);
    }
  }

  private static void setTextWithBuffer(TextView textView, CharArrayBuffer buffer) {
    textView.setText(buffer.data, 0, buffer.sizeCopied);
  }

  private static class APODPostsQuery {
    public static String[] PROJECTION = {
        APODContract.Columns._ID,
        APODContract.Columns.TITLE,
        APODContract.Columns.DESCRIPTION_TEXT,
        APODContract.Columns.LARGE_IMAGE_URL,
    };

    public static final int ID_INDEX = 0;
    public static final int TITLE_INDEX = 1;
    public static final int DESCRIPTION_TEXT_INDEX = 2;
    public static final int LARGE_IMAGE_URL_INDEX = 3;

    public static CursorLoader createCursorLoader(Context context) {
      return new CursorLoader(
          context,
          APODContract.CONTENT_URI,
          PROJECTION,
          null /* selection */,
          null /* selectionArgs */,
          null /* sortOrder */);
    }
  }
}

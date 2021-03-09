/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.sample;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.util.Log;

import com.facebook.stetho.DumperPluginsProvider;
import com.facebook.stetho.InspectorModulesProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.dumpapp.DumperPlugin;
import com.facebook.stetho.inspector.database.ContentProviderDatabaseDriver;
import com.facebook.stetho.inspector.database.ContentProviderSchema;
import com.facebook.stetho.inspector.database.ContentProviderSchema.Table;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;

public class SampleDebugApplication extends SampleApplication {
  private static final String TAG = "SampleDebugApplication";

  @Override
  public void onCreate() {
    super.onCreate();

    long startTime = SystemClock.elapsedRealtime();
    initializeStetho(this);
    long elapsed = SystemClock.elapsedRealtime() - startTime;
    Log.i(TAG, "Stetho initialized in " + elapsed + " ms");
  }

  private void initializeStetho(final Context context) {
    // See also: Stetho.initializeWithDefaults(Context)
    Stetho.initialize(Stetho.newInitializerBuilder(context)
        .enableDumpapp(new DumperPluginsProvider() {
          @Override
          public Iterable<DumperPlugin> get() {
            return new Stetho.DefaultDumperPluginsBuilder(context)
                .provide(new HelloWorldDumperPlugin())
                .provide(new APODDumperPlugin(context.getContentResolver()))
                .finish();
          }
        })
        .enableWebKitInspector(new ExtInspectorModulesProvider(context))
        .build());
  }

  private static class ExtInspectorModulesProvider implements InspectorModulesProvider {

    private Context mContext;

    ExtInspectorModulesProvider(Context context) {
      mContext = context;
    }

    @Override
    public Iterable<ChromeDevtoolsDomain> get() {
      return new Stetho.DefaultInspectorModulesBuilder(mContext)
          .provideDatabaseDriver(createContentProviderDatabaseDriver(mContext))
          .finish();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private ContentProviderDatabaseDriver createContentProviderDatabaseDriver(Context context) {
      ContentProviderSchema calendarsSchema = new ContentProviderSchema.Builder()
          .table(new Table.Builder()
              .uri(CalendarContract.Calendars.CONTENT_URI)
              .projection(new String[] {
                  CalendarContract.Calendars._ID,
                  CalendarContract.Calendars.NAME,
                  CalendarContract.Calendars.ACCOUNT_NAME,
                  CalendarContract.Calendars.IS_PRIMARY,
              })
              .build())
          .build();

      // sample events content provider we want to support
      ContentProviderSchema eventsSchema = new ContentProviderSchema.Builder()
          .table(new Table.Builder()
              .uri(CalendarContract.Events.CONTENT_URI)
              .projection(new String[]{
                  CalendarContract.Events._ID,
                  CalendarContract.Events.TITLE,
                  CalendarContract.Events.DESCRIPTION,
                  CalendarContract.Events.ACCOUNT_NAME,
                  CalendarContract.Events.DTSTART,
                  CalendarContract.Events.DTEND,
                  CalendarContract.Events.CALENDAR_ID,
              })
              .build())
          .build();
      return new ContentProviderDatabaseDriver(context, calendarsSchema, eventsSchema);
    }
  }

}

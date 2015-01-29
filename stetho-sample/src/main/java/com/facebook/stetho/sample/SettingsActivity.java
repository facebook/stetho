// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
  public static void show(Context context) {
    context.startActivity(new Intent(context, SettingsActivity.class));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Trying to avoid a dependency on the support library and go all the way back to Gingerbread,
    // so we can't rely on the fragment-based preferences and must use the old deprecated methods.
    addPreferencesFromResource(R.xml.settings);
  }
}

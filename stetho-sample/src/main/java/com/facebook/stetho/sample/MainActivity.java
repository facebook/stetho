/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.sample;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import timber.log.Timber;

public class MainActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    // Demonstrate that it is removed from the release build...
    if (!isStethoPresent()) {
      Toast.makeText(
          this,
          getString(R.string.stetho_missing, BuildConfig.BUILD_TYPE),
          Toast.LENGTH_LONG)
          .show();
    }

    findViewById(R.id.log_btn).setOnClickListener(v -> {
      Timber.i("log button clicked");
    });
    findViewById(R.id.settings_btn).setOnClickListener(v -> {
      SettingsActivity.show(MainActivity.this);
    });
    findViewById(R.id.apod_btn).setOnClickListener(v -> {
      APODActivity.show(MainActivity.this);
    });
    findViewById(R.id.irc_btn).setOnClickListener(v -> {
      IRCConnectActivity.show(MainActivity.this);
    });
    findViewById(R.id.about).setOnClickListener(v -> {
      View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_layout, null);
      Dialog dialog = new Dialog(MainActivity.this);
      dialog.setContentView(view);
      dialog.setTitle(getString(R.string.app_name));
      dialog.show();
    });
  }

  private static boolean isStethoPresent() {
    try {
      Class.forName("com.facebook.stetho.Stetho");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    getPrefs().registerOnSharedPreferenceChangeListener(mToastingPrefListener);
  }

  @Override
  protected void onPause() {
    super.onPause();
    getPrefs().unregisterOnSharedPreferenceChangeListener(mToastingPrefListener);
  }

  private SharedPreferences getPrefs() {
    return PreferenceManager.getDefaultSharedPreferences(this /* context */);
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener mToastingPrefListener =
      new SharedPreferences.OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      Object value = sharedPreferences.getAll().get(key);
      Toast.makeText(
          MainActivity.this,
          getString(R.string.pref_change_message, key, value),
          Toast.LENGTH_SHORT).show();
    }
  };
}

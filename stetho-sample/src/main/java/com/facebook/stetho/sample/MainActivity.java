// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.sample;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    findViewById(R.id.settings_btn).setOnClickListener(mMainButtonClicked);
    findViewById(R.id.apod_btn).setOnClickListener(mMainButtonClicked);
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

  private final View.OnClickListener mMainButtonClicked = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      int id = v.getId();
      if (id == R.id.settings_btn) {
        SettingsActivity.show(MainActivity.this);
      } else if (id == R.id.apod_btn) {
        APODActivity.show(MainActivity.this);
      }
    }
  };

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

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

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

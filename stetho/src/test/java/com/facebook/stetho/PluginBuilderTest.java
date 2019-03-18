/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho;

import android.app.Activity;
import android.os.Build;
import com.facebook.stetho.dumpapp.DumperPlugin;
import com.facebook.stetho.dumpapp.plugins.HprofDumperPlugin;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.module.Debugger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.junit.Assert.assertFalse;

@Config(emulateSdk = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(RobolectricTestRunner.class)
public class PluginBuilderTest {
  private final Activity mActivity = Robolectric.setupActivity(Activity.class);

  @Test
  public void test_Remove_DefaultInspectorModulesBuilder() throws IOException {
    final Class<Debugger> debuggerClass = Debugger.class;

    Iterable<ChromeDevtoolsDomain> domains = new Stetho.DefaultInspectorModulesBuilder(mActivity)
            .remove(debuggerClass.getName())
            .finish();

    boolean containsDebugggerDomain = false;
    for (ChromeDevtoolsDomain domain : domains) {
      if (domain.getClass().equals(debuggerClass)) {
        containsDebugggerDomain = true;
        break;
      }
    }

    assertFalse(containsDebugggerDomain);
  }

  @Test
  public void test_Remove_DefaultDumperPluginsBuilder() throws IOException {
    //HprofDumperPlugin.NAME is private
    final String hprofDumperPluginNAME = "hprof";
    final Iterable<DumperPlugin> dumperPlugins = new Stetho.DefaultDumperPluginsBuilder(mActivity)
            .remove(hprofDumperPluginNAME)
            .finish();

    boolean containsDebugggerDomain = false;
    for (DumperPlugin plugin : dumperPlugins) {
      if (plugin.getClass().equals(HprofDumperPlugin.class)) {
        containsDebugggerDomain = true;
        break;
      }
    }

    assertFalse(containsDebugggerDomain);
  }

}

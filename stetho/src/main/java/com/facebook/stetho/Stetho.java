/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho;

import com.facebook.stetho.dumpapp.plugins.CrashDumperPlugin;
import com.facebook.stetho.dumpapp.plugins.FilesDumperPlugin;
import com.facebook.stetho.dumpapp.plugins.HprofDumperPlugin;
import com.facebook.stetho.inspector.database.DefaultDatabaseFilesProvider;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Utf8Charset;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.dumpapp.Dumper;
import com.facebook.stetho.dumpapp.DumperPlugin;
import com.facebook.stetho.dumpapp.RawDumpappHandler;
import com.facebook.stetho.dumpapp.StreamingDumpappHandler;
import com.facebook.stetho.dumpapp.plugins.SharedPreferencesDumperPlugin;
import com.facebook.stetho.inspector.ChromeDevtoolsServer;
import com.facebook.stetho.inspector.ChromeDiscoveryHandler;
import com.facebook.stetho.inspector.database.SqliteDatabaseDriver;
import com.facebook.stetho.inspector.elements.Document;
import com.facebook.stetho.inspector.elements.android.ActivityTracker;
import com.facebook.stetho.inspector.elements.android.AndroidDOMConstants;
import com.facebook.stetho.inspector.elements.android.AndroidDocumentProviderFactory;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.module.CSS;
import com.facebook.stetho.inspector.protocol.module.Console;
import com.facebook.stetho.inspector.protocol.module.DOM;
import com.facebook.stetho.inspector.protocol.module.DOMStorage;
import com.facebook.stetho.inspector.protocol.module.Database;
import com.facebook.stetho.inspector.protocol.module.DatabaseConstants;
import com.facebook.stetho.inspector.protocol.module.Debugger;
import com.facebook.stetho.inspector.protocol.module.HeapProfiler;
import com.facebook.stetho.inspector.protocol.module.Inspector;
import com.facebook.stetho.inspector.protocol.module.Network;
import com.facebook.stetho.inspector.protocol.module.Page;
import com.facebook.stetho.inspector.protocol.module.Profiler;
import com.facebook.stetho.inspector.protocol.module.Runtime;
import com.facebook.stetho.inspector.protocol.module.Worker;
import com.facebook.stetho.server.LocalSocketHttpServer;
import com.facebook.stetho.server.RegistryInitializer;
import com.facebook.stetho.websocket.WebSocketHandler;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;

/**
 * Initialization and configuration entry point for the Stetho debugging system.  Simple usage with
 * default plugins and features enabled:
 * <p />
 * <pre>
 *   Stetho.initializeWithDefaults(context)
 * </pre>
 * <p />
 * For more advanced configuration, see {@link #newInitializerBuilder(Context)} or
 * the {@code stetho-sample} for more information.
 */
public class Stetho {
  private static final String LISTENER_THREAD_NAME = "Stetho-Listener";

  private Stetho() {
  }

  /**
   * Construct a simple initializer helper which allows you to customize stetho behaviour
   * with additional features, plugins, etc.  See {@link DefaultDumperPluginsBuilder} and
   * {@link DefaultInspectorModulesBuilder} for more information.
   * <p />
   * For simple use cases, consider {@link #initializeWithDefaults(Context)}.
   */
  public static InitializerBuilder newInitializerBuilder(Context context) {
    return new InitializerBuilder(context);
  }

  /**
   * Start the listening server.  Most of the heavy lifting initialization is deferred until the
   * first socket connection is received, allowing this to be safely used for debug builds on
   * even low-end hardware without noticeably affecting performance.
   */
  public static void initializeWithDefaults(final Context context) {
    initialize(new Initializer(context) {
      @Override
      protected Iterable<DumperPlugin> getDumperPlugins() {
        return new DefaultDumperPluginsBuilder(context).finish();
      }

      @Override
      protected Iterable<ChromeDevtoolsDomain> getInspectorModules() {
        return new DefaultInspectorModulesBuilder(context).finish();
      }
    });
  }

  /**
   * Start the listening service, providing a custom initializer as per
   * {@link #newInitializerBuilder}.
   *
   * @see #initializeWithDefaults(Context)
   */
  public static void initialize(final Initializer initializer) {
    // Hook activity tracking so that after Stetho is attached we can figure out what
    // activities are present.
    boolean isTrackingActivities = ActivityTracker.get().beginTrackingIfPossible(
        (Application)initializer.mContext.getApplicationContext());
    if (!isTrackingActivities) {
      LogUtil.w("Automatic activity tracking not available on this API level, caller must invoke " +
          "ActivityTracker methods manually!");
    }

    Thread listener = new Thread(LISTENER_THREAD_NAME) {
      @Override
      public void run() {
        LocalSocketHttpServer server = new LocalSocketHttpServer(initializer);
        try {
          server.run();
        } catch (IOException e) {
          LogUtil.e(e, "Could not start Stetho");
        }
      }
    };
    listener.start();
  }

  public static DumperPluginsProvider defaultDumperPluginsProvider(final Context context) {
    return new DumperPluginsProvider() {
      @Override
      public Iterable<DumperPlugin> get() {
        return new DefaultDumperPluginsBuilder(context).finish();
      }
    };
  }

  public static InspectorModulesProvider defaultInspectorModulesProvider(final Context context) {
    return new InspectorModulesProvider() {
      @Override
      public Iterable<ChromeDevtoolsDomain> get() {
        return new DefaultInspectorModulesBuilder(context).finish();
      }
    };
  }

  private static class PluginBuilder<T> {
    private final Set<String> mProvidedNames = new HashSet<>();
    private final Set<String> mRemovedNames = new HashSet<>();

    private final ArrayList<T> mPlugins = new ArrayList<>();

    private boolean mFinished;

    public void provide(String name, T plugin) {
      throwIfFinished();
      mPlugins.add(plugin);
      mProvidedNames.add(name);
    }

    public void provideIfDesired(String name, T plugin) {
      throwIfFinished();
      if (!mRemovedNames.contains(name)) {
        if (mProvidedNames.add(name)) {
          mPlugins.add(plugin);
        }
      }
    }

    public void remove(String pluginName) {
      throwIfFinished();
      mRemovedNames.remove(pluginName);
    }

    private void throwIfFinished() {
      if (mFinished) {
        throw new IllegalStateException("Must not continue to build after finish()");
      }
    }

    public Iterable<T> finish() {
      mFinished = true;
      return mPlugins;
    }
  }

  public static final class DefaultDumperPluginsBuilder {
    private final Context mContext;
    private final PluginBuilder<DumperPlugin> mDelegate = new PluginBuilder<>();

    public DefaultDumperPluginsBuilder(Context context) {
      mContext = context;
    }

    public DefaultDumperPluginsBuilder provide(DumperPlugin plugin) {
      mDelegate.provide(plugin.getName(), plugin);
      return this;
    }

    private DefaultDumperPluginsBuilder provideIfDesired(DumperPlugin plugin) {
      mDelegate.provideIfDesired(plugin.getName(), plugin);
      return this;
    }

    public DefaultDumperPluginsBuilder remove(String pluginName) {
      mDelegate.remove(pluginName);
      return this;
    }

    public Iterable<DumperPlugin> finish() {
      provideIfDesired(new HprofDumperPlugin(mContext));
      provideIfDesired(new SharedPreferencesDumperPlugin(mContext));
      provideIfDesired(new CrashDumperPlugin());
      provideIfDesired(new FilesDumperPlugin(mContext));
      return mDelegate.finish();
    }
  }

  public static final class DefaultInspectorModulesBuilder {
    private final Context mContext;
    private final PluginBuilder<ChromeDevtoolsDomain> mDelegate = new PluginBuilder<>();

    public DefaultInspectorModulesBuilder(Context context) {
      mContext = context;
    }

    public DefaultInspectorModulesBuilder provide(ChromeDevtoolsDomain module) {
      mDelegate.provide(module.getClass().getName(), module);
      return this;
    }

    private DefaultInspectorModulesBuilder provideIfDesired(ChromeDevtoolsDomain module) {
      mDelegate.provideIfDesired(module.getClass().getName(), module);
      return this;
    }

    public DefaultInspectorModulesBuilder remove(String moduleName) {
      mDelegate.remove(moduleName);
      return this;
    }

    public Iterable<ChromeDevtoolsDomain> finish() {
      provideIfDesired(new Console());
      provideIfDesired(new CSS());
      provideIfDesired(new Debugger());
      if (Build.VERSION.SDK_INT >= AndroidDOMConstants.MIN_API_LEVEL) {
        Document document = new Document(
            new AndroidDocumentProviderFactory(
                (Application) mContext.getApplicationContext()));

        provideIfDesired(new DOM(document));
      }
      provideIfDesired(new DOMStorage(mContext));
      provideIfDesired(new HeapProfiler());
      provideIfDesired(new Inspector());
      provideIfDesired(new Network(mContext));
      provideIfDesired(new Page(mContext));
      provideIfDesired(new Profiler());
      provideIfDesired(new Runtime());
      provideIfDesired(new Worker());
      if (Build.VERSION.SDK_INT >= DatabaseConstants.MIN_API_LEVEL) {
        Database database = new Database();
        database.add(new SqliteDatabaseDriver(mContext, new DefaultDatabaseFilesProvider(mContext)));
        provideIfDesired(database);
      }
      return mDelegate.finish();
    }
  }

  /**
   * Callers can choose to subclass this directly to provide the initialization configuration
   * or they can construct a concrete instance using {@link #newInitializerBuilder(Context)}.
   */
  public static abstract class Initializer implements RegistryInitializer {
    private final Context mContext;

    protected Initializer(Context context) {
      mContext = context.getApplicationContext();
    }

    @Override
    public final HttpRequestHandlerRegistry getRegistry() {
      HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();

      Iterable<DumperPlugin> dumperPlugins = getDumperPlugins();
      if (dumperPlugins != null) {
        Dumper dumper = new Dumper(dumperPlugins);

        registry.register("/dumpapp", new StreamingDumpappHandler(mContext, dumper));
        registry.register("/dumpapp-raw", new RawDumpappHandler(mContext, dumper));
      }

      Iterable<ChromeDevtoolsDomain> inspectorModules = getInspectorModules();
      if (inspectorModules != null) {
        ChromeDiscoveryHandler discoveryHandler =
            new ChromeDiscoveryHandler(
                mContext,
                ChromeDevtoolsServer.PATH);
        discoveryHandler.register(registry);
        registry.register(
            ChromeDevtoolsServer.PATH,
            new WebSocketHandler(mContext, new ChromeDevtoolsServer(inspectorModules)));
      }

      addCustomEntries(registry);

      registry.register("/*", new LoggingCatchAllHandler());

      return registry;
    }

    @Nullable
    protected abstract Iterable<DumperPlugin> getDumperPlugins();

    @Nullable
    protected abstract Iterable<ChromeDevtoolsDomain> getInspectorModules();

    protected void addCustomEntries(HttpRequestHandlerRegistry registry) {
      // Override to add stuff...
    }

    private static class LoggingCatchAllHandler implements HttpRequestHandler {
      @Override
      public void handle(
          HttpRequest request,
          HttpResponse response,
          HttpContext context)
          throws HttpException, IOException {
        LogUtil.w("Unsupported request received: " + request.getRequestLine());
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("Not Found");
        response.setEntity(new StringEntity("Endpoint not implemented\n", Utf8Charset.NAME));
      }
    }
  }

  /**
   * Configure what services are to be enabled in this instance of Stetho.
   */
  public static class InitializerBuilder {
    final Context mContext;

    @Nullable DumperPluginsProvider mDumperPlugins;
    @Nullable InspectorModulesProvider mInspectorModules;

    private InitializerBuilder(Context context) {
      mContext = context.getApplicationContext();
    }

    /**
     * Enable use of the {@code dumpapp} system.  This is an extension to Stetho which allows
     * developers to configure custom debug endpoints as tiny programs embedded inside of a larger
     * running Android application.  Examples of this would be simple utilities to visualize and
     * edit {@link SharedPreferences} data, kick off sync or other background tasks, inject custom
     * data temporarily into the process for debugging/reproducibility, upload error reports,
     * etc.
     * <p>
     * See {@code ./scripts/dumpapp} for more information on how to use this system once
     * enabled.
     *
     * @param plugins The set of plugins to use.
     */
    public InitializerBuilder enableDumpapp(DumperPluginsProvider plugins) {
      mDumperPlugins = Util.throwIfNull(plugins);
      return this;
    }

    public InitializerBuilder enableWebKitInspector(InspectorModulesProvider modules) {
      mInspectorModules = modules;
      return this;
    }

    public Initializer build() {
      return new BuilderBasedInitializer(this);
    }
  }

  private static class BuilderBasedInitializer extends Initializer {
    @Nullable private final DumperPluginsProvider mDumperPlugins;
    @Nullable private final InspectorModulesProvider mInspectorModules;

    private BuilderBasedInitializer(InitializerBuilder b) {
      super(b.mContext);
      mDumperPlugins = b.mDumperPlugins;
      mInspectorModules = b.mInspectorModules;
    }

    @Nullable
    @Override
    protected Iterable<DumperPlugin> getDumperPlugins() {
      return mDumperPlugins != null ? mDumperPlugins.get() : null;
    }

    @Nullable
    @Override
    protected Iterable<ChromeDevtoolsDomain> getInspectorModules() {
      return mInspectorModules != null ? mInspectorModules.get() : null;
    }
  }
}

package com.facebook.stetho;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;

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
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.module.CSS;
import com.facebook.stetho.inspector.protocol.module.Console;
import com.facebook.stetho.inspector.protocol.module.DOM;
import com.facebook.stetho.inspector.protocol.module.DOMStorage;
import com.facebook.stetho.inspector.protocol.module.Database;
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
 * Initialization and configuration entry point for the Stetho debugging system.  Example usage:
 * <p>
 * <pre>
 *   Stetho.initialize(Stetho.newInitializerBuilder(context)
 *       .enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
 *       .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(context))
 *       .build());
 * </pre>
 */
public class Stetho {
  private static final String LISTENER_THREAD_NAME = "Stetho-Listener";

  private Stetho() {
  }

  public static InitializerBuilder newInitializerBuilder(Context context) {
    return new InitializerBuilder(context);
  }

  /**
   * Start the listening server.  Most of the heavy lifting initialization is deferred until the
   * first socket connection is received, allowing this to be safely used for debug builds on
   * even low-end hardware without noticeably affecting performance.
   */
  public static void initialize(final Initializer initializer) {
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
        ArrayList<DumperPlugin> plugins = new ArrayList<DumperPlugin>();
        plugins.add(new SharedPreferencesDumperPlugin(context));
        return plugins;
      }
    };
  }

  public static InspectorModulesProvider defaultInspectorModulesProvider(final Context context) {
    return new InspectorModulesProvider() {
      @Override
      public Iterable<ChromeDevtoolsDomain> get() {
        ArrayList<ChromeDevtoolsDomain> modules = new ArrayList<ChromeDevtoolsDomain>();
        modules.add(new Console());
        modules.add(new CSS());
        modules.add(new Debugger());
        modules.add(new DOM());
        modules.add(new DOMStorage(context));
        modules.add(new HeapProfiler());
        modules.add(new Inspector());
        modules.add(new Network(context));
        modules.add(new Page(context));
        modules.add(new Profiler());
        modules.add(new Runtime());
        modules.add(new Worker());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          modules.add(new Database(context));
        }
        return modules;
      }
    };
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

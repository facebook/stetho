# Stetho
Stetho is a sophisticated debug bridge for Android applications. When enabled,
developers have access to the Chrome Developer Tools feature natively part of
the Chrome desktop browser. Developers can also choose to enable the optional
`dumpapp` tool which offers a powerful command-line interface to application
internals.

## Features

### WebKit Inspector
WebKit Inspector is the internal name of the Chrome Developer Tools feature.
It is implemented using a client/server protocol which the Stetho software
provides for your application.  Once your application is integrated, simply
navigate to `chrome://inspect` and click "Inspect" to get started!

![Inspector Discovery Screenshot](https://github.com/facebook/stetho/raw/master/docs/images/inspector-discovery.png)

#### Network inspection
Network inspection is possible with the full spectrum of Chrome Developer Tools features, including image preview, JSON response helpers, and even exporting traces to the HAR format.

![Inspector Network Screenshot](https://github.com/facebook/stetho/raw/master/docs/images/inspector-network.png)

#### Database inspection
SQLite databases can be visualized and interactively explored with full read/write capabilities.

![Inspector WebSQL Screenshot](https://github.com/facebook/stetho/raw/master/docs/images/inspector-sqlite.png)

### dumpapp
Dumpapp extends beyond the Inspector UI features shown above to provide a much
more extensible, command-line interface to application components.  A default
set of plugins is provided, but the real power of dumpapp is the ability to
easily create your own!

![dumpapp prefs Screenshot](https://github.com/facebook/stetho/raw/master/docs/images/dumpapp-prefs.png)

## Integration

### Add library code
Assemble the JAR files locally using:

```shell
./gradlew assemble
```

Copy the relevant jar files to your `libs/` directory.  Only the main `stetho`
jar file is strictly required, however you may wish to copy
`stetho-urlconnection` or `stetho-okhttp` for simplified network integration.
Also take note that you will need to rename the jar files to avoid conflicts.
The jar files are located in their respective `build` directories:

```shell
./stetho/build/intermediates/bundles/debug/classes.jar
./stetho-urlconnection/build/intermediates/bundles/debug/classes.jar
./stetho-okhttp/build/intermediates/bundles/debug/classes.jar
```

You will also need Apache's `commons-cli` library, which you can access from
`build.gradle`:

```groovy
compile 'commons-cli:commons-cli:1.2'
```

### Set-up
Integrating with Stetho is intended to be seamless and straightforward for
most existing Android applications.  There is a simple initialization step
which occurs in your `Application` class:

```java
public class MyApplication extends Application {
  public void onCreate() {
    super.onCreate();
    Stetho.initialize(
        Stetho.newInitializerBuilder(this)
            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
            .build());
  }
}
```

This brings up most of the default configuration but does not enable some
additional hooks (most notably, network inspection).  See below for specific
details on individual subsystems.

### Enable network inspection
If you are using the popular [OkHttp](http://https://github.com/square/okio)
library at the 2.2.x+ release, you can use the
[Interceptors](https://github.com/square/okhttp/wiki/Interceptors) system to
automatically hook into your existing stack.  This is currently the simplest
and most straightforward way to enable network inspection:

```java
OkHttpClient client = new OkHttpClient();
client.networkInterceptors().add(new StethoInterceptor());
```

If you are using any of other network stack options, you will need to manually
provide data to the `NetworkEventReporter` interface.  The general pattern for implementing this is:

```java
NetworkEventReporter reporter = NetworkEventReporterImpl.get();
// Important to check if it is enabled first so as not to add overhead to
// the common case that is not under scrutiny.
if (reporter.isEnabled()) {
  reporter.requestWillBeSent(new MyInspectorRequest(request));
}
```

See the `stetho-sample` project for more details.

### Custom dumpapp plugins
Custom plugins are the preferred means of extending the `dumpapp` system and
can be added easily during configuration.  Simply replace your configuration
step as such:

```java
Stetho.initialize(Stetho.newInitializerBuilder(context)
    .enableDumpapp(new MyDumperPluginsProvider(context))
    .build())

private static class MyDumperPluginsProvider implements DumperPluginsProvider {
  ...

  public Iterable<DumperPlugin> get() {
    ArrayList<DumperPlugin> plugins = new ArrayList<DumperPlugin>();
    for (DumperPlugin defaultPlugin : Stetho.defaultDumperPluginsProvider(mContext).get()) {
      plugins.add(defaultPlugin);
    }
    plugins.add(new MyDumperPlugin());
    return plugins;
  }
}
```

See the `stetho-sample` project for more details.

## Improve Stetho!
See the CONTRIBUTING.md file for how to help out.

## License
Stetho is BSD-licensed. We also provide an additional patent grant.

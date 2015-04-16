---
layout: home
id: home
---

## Download

{% include ui/button.html button_href="https://github.com/facebook/stetho/releases/download/v1.1.0/stetho-1.1.0-fatjar.jar" button_text="Download v1.1.0" margin="small" align="center" %}

Alternatively you can include Stetho from Maven Central via Gradle or Maven. 

```groovy
  // Gradle dependency on Stetho 
  dependencies { 
    compile 'com.facebook.stetho:stetho:1.1.0' 
  } 
```

```xml
  <dependency>
    <groupid>com.facebook.stetho</groupid> 
    <artifactid>stetho</artifactid> 
    <version>1.1.0</version> 
  </dependency> 
```

Only the main `stetho` dependency is strictly required, however you may also wish to use one of the network helpers: 

```groovy 
  dependencies { 
    compile 'com.facebook.stetho:stetho-okhttp:1.1.0' 
  } 
```

or: 

```groovy
  dependencies { 
    compile 'com.facebook.stetho:stetho-urlconnection:1.1.0' 
  } 
```

##Features 

{% include content/gridblocks.html data_source=site.data.features grid_type="twoByGridBlock" %}

## Integrations

### Setup

Integrating with **Stetho** is intended to be seamless and straightforward for most existing Android applications. There is a simple initialization step which occurs in your `Application` class:
    
```java    
public class MyApplication extends Application {
  public void onCreate() {
    super.onCreate();
    Stetho.initialize(
      Stetho.newInitializerBuilder(this)
        .enableDumpapp(
            Stetho.defaultDumperPluginsProvider(this))
        .enableWebKitInspector(
            Stetho.defaultInspectorModulesProvider(this))
        .build());
  }
}
```

This brings up most of the default configuration but does not enable some additional hooks (most notably, network inspection). See below for specific details on individual subsystems.

### Enable Network Inspection

If you are using the popular OkHttp library at the 2.2.x+ release, you can use the Interceptors system to automatically hook into your existing stack. This is currently the simplest and most straightforward way to enable network inspection: 
    
```java    
OkHttpClient client = new OkHttpClient();
client.networkInterceptors().add(new StethoInterceptor());
```

If you are using `HttpURLConnection`, you can use `StethoURLConnectionManager` to assist with integration though you should be aware that there are some caveats with this approach. In particular, you must explicitly add `Accept-Encoding: gzip` to the request headers and manually handle compressed responses in order for Stetho to report compressed payload sizes.

See the stetho-sample project for more details. 

### Custom dumpapp Plugins

Custom plugins are the preferred means of extending the dumpapp system and can be added easily during configuration. Simply replace your configuration step as such: 
  
```java  
Stetho.initialize(Stetho.newInitializerBuilder(context)
    .enableDumpapp(new MyDumperPluginsProvider(context))
    .build())

private static class MyDumperPluginsProvider
    implements DumperPluginsProvider {
  public Iterable<DumperPlugin> get() {
    ArrayList<DumperPlugin> plugins = new ArrayList<DumperPlugin>();
    for (DumperPlugin defaultPlugin :
        Stetho.defaultDumperPluginsProvider(mContext).get()) {
      plugins.add(defaultPlugin);
    }
    plugins.add(new MyDumperPlugin());
    return plugins;
  }
}
```

See the stetho-sample project for more details. 

### Contributions
Use [Github issues](https://github.com/facebook/stetho/issues) for requests. We actively welcome pull requests; learn how to [contribute](https://github.com/facebook/stetho/blob/master/CONTRIBUTING.md).

###Changelog
Changes are tracked as [Github releases](https://github.com/facebook/stetho/releases).

###License  

Stetho is [BSD-licensed](https://github.com/facebook/stetho/blob/master/LICENSE). We also provide an additional [patent grant](https://github.com/facebook/stetho/blob/master/PATENTS).

{% include plugins/all_share.html %}

# Stetho's JavaScript Module

This [Stetho](https://facebook.github.io/stetho) plugin adds a JavaScript console by embedding Mozilla's [Rhino](https://github.com/mozilla/rhino).

## Set-up

### Download
Download [the latest JARs](https://github.com/facebook/stetho/releases/latest) or grab via Gradle:
```groovy
implementation 'com.facebook.stetho:stetho-js-rhino:1.4.2'
```
or Maven:
```xml
<dependency>
  <groupId>com.facebook.stetho</groupId>
  <artifactId>stetho-js-rhino</artifactId>
  <version>1.4.2</version>
</dependency>
```

Make sure that you depend on the main `stetho` dependency too.

### Putting it together

The Rhino JavaScript integration is automatically detected by Stetho and is
enabled simply by adding the `stetho-js-rhino` dependency to your project.

If you want to configure the JavaScript environment you can pass your own
variables, classes, packages and functions and provide this custom runtime REPL using:

```java
    Stetho.initialize(Stetho.newInitializerBuilder(context)
        .enableWebKitInspector(new InspectorModulesProvider() {
          @Override
          public Iterable<ChromeDevtoolsDomain> get() {
            return new DefaultInspectorModulesBuilder(context).runtimeRepl(
                new JsRuntimeReplFactoryBuilder(context)
                    // Pass to JavaScript: var foo = "bar";
                    .addVariable("foo", "bar")
                    .build()
            ).finish();
          }
        })
        .build();
```

For more details see the next sections.

### How it works

At the core this plugin initializes a JavaScript runtime provided by Mozilla's [Rhino](https://github.com/mozilla/rhino).
The runtime is configured so that it will work within an Android application.
This means that code has to run in interpreted mode as the more aggressive optimizations performed
by Rhino are done through on-the-fly JVM bytecode generation and this won't work in Android which expects Dalvik bytecode.

For generic purposes the interpreted mode should have no performance impact since this is debug tool.

## Customization

By default a JavaScript interpreter starts with an empty scope (environment) and has no default variables or functions set besides the ones described by the language specification.

You might be used to the browser setting up various objects for your like `document`, `console`, etc. This is something that's not part of the javascript specification and is particular only
to the browser's runtime.

## Example

Once you have enabled the JavaScript console in your app you will be able to run live JavaScript commands on your app from the console.

Here's an example to show a toast from the console:

```javascript
importPackage(android.widget);
importPackage(android.os);
var handler = new Handler(Looper.getMainLooper());
handler.post(function() { Toast.makeText(context, "Hello from JavaScript", Toast.LENGTH_LONG).show() });
```

### Default scope

Rhino offers the possibility to use an enhanced runtime where some utilities have been added
in order to help the integration with the java runtime.
This is exactly the runtime that the plugin uses.
The default scope used by the plugin is described in more details in the article [Scripting Java](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scripting_Java).

The scope can be enhanced by your application, if desired.
You can preload classes and packages, bind variables, objects and even functions.
This means that your java classes and objects can be accessed from JavaScript.

### Builtins

The JavaScript runtime use by this plugin has been enhanced.
By default your application's package has been imported.
So things like `R.string.app` should work.

The functions `importClass` and `importPackage` have been added.

A `console` object is available too. It supports only a `log()` method for now.

### Import a class

First define a JsRuntimeReplFactoryBuilder object:

```
// context is your application context
JsRuntimeReplFactoryBuilder jsRuntimeBuilder = new JsRuntimeReplFactoryBuilder(context);
```

To import a java class into the JavaScript runtime do:

```java
jsRuntimeBuilder.importClass(R.class);
```

### Import a package

To import all classes in a java package into the JavaScript runtime do:

```java
jsRuntimeBuilder.importPackage("android.content");
```

### Variable binding

Here's how to pass a variable to the JavaScript runtime:

```java
jsRuntimeBuilder.addVariable("flag", new AtomicBoolean(true));
```
**Note**: Java primitive types will be autoboxed, only objects can be passed to the javascript runtime.

### Function binding

You can also add custom javascript functions that will be available to the runtime.
This requires a bit more of work on your part.
Remember that you invoke methods on objects that you have already binded.

If you really want to define a top level function this is how it can be done:

```java
// Your application context
final Context context = this;

final Handler handler = new Handler(Looper.getMainLooper());

// Add the function: void toast(String)
jsRuntimeBuilder.addFunction("toast", new BaseFunction() {
  @Override
  public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    // javascript passes the arguments as varags
    final String message = args[0].toString();
    handler.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
      }
    });

    // return undef in javascript
    return org.mozilla.javascript.Context.getUndefinedValue();
  }
});
```

**Note**: This is a complex example since a toast be invoked from the main UI thread.

## Limitations

As mentioned in the section [how it works](#how-it-works) the JavaScript runtime has to run in interpreted mode due to the nature of the android runtime.

### Dex method count

Rhino is not a small library and it can increase your dex method count by more than 7,000.
The standard Rhino distribution includes a "tools" package that's not required by this plugin
but it is still bundled.
That package alone adds more than 1,200 methods to the dex count.

Hopefully the Rhino devs will split the distribution in smaller artifacts (see https://github.com/mozilla/rhino/issues/156).
In the meanwhile you should consider using proguard to shrink the dex method count.

Here's the dex count of the stetho-sample compiled under various scenarios:

|      | Original | JavaScript |
| :--- | -------: | ---------: |
| Dex  | 15,461   | 22,749     |
| Size | 1.0M     | 1.6M       |

With proguard:

|      | Original | JavaScript | JavaScript (w/o *tools*)     |
| :--- | -------: | ---------: | ---------------------------: |
| Dex  | 8,013    | 15,316     | 14,043                       |
| Size | 847K     | 1.5M       | 1.4M                         |

### Proguard

To proguard your project add the following rules to your proguard file:

```
# stetho
+keep class com.facebook.stetho.** { *; }

# rhino (javascript)
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**
-keep class org.mozilla.javascript.** { *; }
```

If you want to remove the *tools* package for a more aggressive proguard use:

```
# stetho
+keep class com.facebook.stetho.** { *; }

# rhino (javascript)
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**
-keep class org.mozilla.classfile.** { *; }
-keep class org.mozilla.javascript.* { *; }
-keep class org.mozilla.javascript.annotations.** { *; }
-keep class org.mozilla.javascript.ast.** { *; }
-keep class org.mozilla.javascript.commonjs.module.** { *; }
-keep class org.mozilla.javascript.commonjs.module.provider.** { *; }
-keep class org.mozilla.javascript.debug.** { *; }
-keep class org.mozilla.javascript.jdk13.** { *; }
-keep class org.mozilla.javascript.jdk15.** { *; }
-keep class org.mozilla.javascript.json.** { *; }
-keep class org.mozilla.javascript.optimizer.** { *; }
-keep class org.mozilla.javascript.regexp.** { *; }
-keep class org.mozilla.javascript.serialize.** { *; }
-keep class org.mozilla.javascript.typedarrays.** { *; }
-keep class org.mozilla.javascript.v8dtoa.** { *; }
-keep class org.mozilla.javascript.xml.** { *; }
-keep class org.mozilla.javascript.xmlimpl.** { *; }
```


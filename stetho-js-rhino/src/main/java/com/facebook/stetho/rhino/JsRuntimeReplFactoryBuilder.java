/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.rhino;

import android.util.Log;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.inspector.console.CLog;
import com.facebook.stetho.inspector.console.RuntimeRepl;
import com.facebook.stetho.inspector.console.RuntimeReplFactory;
import com.facebook.stetho.inspector.protocol.module.Console;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

/**
 * <p>Builder used to setup the javascript runtime to be used by stetho.</p>
 *
 * <p>You can use this builder to configure the javacript environment by preloading:
 * <ul>
 *   <li>Java classes</li>
 *   <li>Java packages (with all their java classes)</li>
 *   <li>Variables</li>
 *   <li>Functions</li>
 * </ul>
 * </p>
 *
 * <p>Your application context package is automatically visible with this builder.</p>
 */
public class JsRuntimeReplFactoryBuilder {

  /**
   * Name of the "source" file used for reporting JavaScript compilation errors (or runtime errors).
   * Since this is evaluated from a chrome inspector window we pass "chrome".
   */
  private static final String SOURCE_NAME = "chrome";

  /**
   * Android application context.
   */
  private final android.content.Context mContext;

  /**
   * Java classes to import into the javascript environment.
   */
  private final Set<Class<?>> mClasses = new HashSet<>();

  /**
   * Java packages to import into the javascript environment.
   * All classes inside the package will be imported.
   */
  private final Set<String> mPackages = new HashSet<>();

  /**
   * Variables to bind to the javascript environment.
   */
  private final Map<String, Object> mVariables = new HashMap<>();

  /**
   * Global mFunctions to add to the javascript environment.
   */
  private final Map<String, Function> mFunctions = new HashMap<>();

  public static RuntimeReplFactory defaultFactory(@NonNull android.content.Context context) {
    return new JsRuntimeReplFactoryBuilder(context).build();
  }

  public JsRuntimeReplFactoryBuilder(@NonNull android.content.Context context) {
    mContext = context;

    // We import the app's package name by default
    mPackages.add(context.getPackageName());

    // Predefine $_ which holds the value of the last expression evaluated
    mVariables.put("$_", Context.getUndefinedValue());
  }

  /**
   * Request that the given java class be imported in the javascript runtime.
   * @param aClass the java class to import
   * @return the builder
   */
  public @NonNull
  JsRuntimeReplFactoryBuilder importClass(@NonNull Class<?> aClass) {
    mClasses.add(aClass);
    return this;
  }

  /**
   * Request that the given package name will be imported in the javascript runtime.
   * This means that all classes (enums and interfaces) will be imported.
   * @param packageName the java package name to import
   * @return the builder
   */
  public @NonNull
  JsRuntimeReplFactoryBuilder importPackage(@NonNull String packageName) {
    mPackages.add(packageName);
    return this;
  }

  /**
   * Add a variable (binding) to the javascript environment.
   * @param name the javascript variable name
   * @param value the value to add
   * @return the builder
   */
  public JsRuntimeReplFactoryBuilder addVariable(@NonNull String name, Object value) {
    mVariables.put(name, value);
    return this;
  }

  /**
   * Adds a function to the javascript environment.
   * @param name the javascript function name
   * @param function the function
   * @return the builder
   */
  public @NonNull
  JsRuntimeReplFactoryBuilder addFunction(@NonNull String name, @NonNull Function function) {
    mFunctions.put(name, function);
    return this;
  }

  /**
   * Build the runtime REPL instance to be supplied to the Stetho {@code Runtime} module.
   */
  public RuntimeReplFactory build() {
    return new RuntimeReplFactory() {
      @Override
      public RuntimeRepl newInstance() {
        return new JsRuntimeRepl(initJsScope());
      }
    };
  }

  /**
   * Initializes a proper javascript scope (runtime environment holding variables).
   * @return a javascript scope
   */
  private @NonNull ScriptableObject initJsScope() {
    final Context jsContext = JsRuntimeRepl.enterJsContext();
    try {
      ScriptableObject scope = initJsScope(jsContext);
      return scope;
    } finally {
      Context.exit();
    }
  }

  private @NonNull ScriptableObject initJsScope(@NonNull Context jsContext) {
    // Set the main Rhino goodies
    ImporterTopLevel importerTopLevel = new ImporterTopLevel(jsContext);
    ScriptableObject scope = jsContext.initStandardObjects(importerTopLevel, false);

    ScriptableObject.putProperty(scope, "context", Context.javaToJS(mContext, scope));

    try {
      importClasses(jsContext, scope);
      importPackages(jsContext, scope);
      importConsole(scope);
      importVariables(scope);
      importFunctions(scope);
    } catch (StethoJsException e) {
      String message = String.format("%s\n%s", e.getMessage(), Log.getStackTraceString(e));
      LogUtil.e(e, message);
      CLog.writeToConsole(Console.MessageLevel.ERROR, Console.MessageSource.JAVASCRIPT, message);
    }

    return scope;
  }

  private void importClasses(@NonNull Context jsContext, @NonNull ScriptableObject scope) throws StethoJsException {
    // Import the classes that the caller requested
    for (Class<?> aClass : mClasses) {
      String className = aClass.getName();
      try {
        // import from default classes
        String expression = String.format("importClass(%s)", className);
        jsContext.evaluateString(scope, expression, SOURCE_NAME, 1, null);
      } catch (Exception e) {
        try {
          // import from application classes
          String expression = String.format("importClass(Packages.%s)", className);
          jsContext.evaluateString(scope, expression, SOURCE_NAME, 1, null);
        } catch (Exception e1) {
          throw new StethoJsException(e1, "Failed to import class: %s", className);
        }
      }
    }
  }

  private void importPackages(@NonNull Context jsContext, @NonNull ScriptableObject scope) throws StethoJsException {
    // Import the packages that the caller requested
    for (String packageName : mPackages) {
      try {
        // import from default packages
        String expression = String.format("importPackage(%s)", packageName);
        jsContext.evaluateString(scope, expression, SOURCE_NAME, 1, null);
      } catch (Exception e) {
        try {
          // import from application packages
          String expression = String.format("importPackage(Packages.%s)", packageName);
          jsContext.evaluateString(scope, expression, SOURCE_NAME, 1, null);
        } catch (Exception e1) {
          throw new StethoJsException(e, "Failed to import package: %s", packageName);
        }
      }
    }
  }

  private void importConsole(@NonNull ScriptableObject scope) throws StethoJsException {
    // Set the `console` object
    try {
      ScriptableObject.defineClass(scope, JsConsole.class);
      JsConsole console = new JsConsole(scope);
      scope.defineProperty("console", console, ScriptableObject.DONTENUM);
    } catch (Exception e) {
      throw new StethoJsException(e, "Failed to setup javascript console");
    }
  }

  private void importVariables(@NonNull ScriptableObject scope) throws StethoJsException {
    // Define the variables
    for (Map.Entry<String, Object> entrySet : mVariables.entrySet()) {
      String varName = entrySet.getKey();
      Object varValue = entrySet.getValue();
      try {
        Object jsValue;
        if (varValue instanceof Scriptable || varValue instanceof Undefined) {
          jsValue = varValue;
        } else {
          jsValue = Context.javaToJS(varValue, scope);
        }
        ScriptableObject.putProperty(scope, varName, jsValue);
      } catch (Exception e) {
        throw new StethoJsException(e, "Failed to setup variable: %s", varName);
      }
    }
  }

  private void importFunctions(@NonNull ScriptableObject scope) throws StethoJsException {
    // Define the functions
    for (Map.Entry<String, Function> entrySet : mFunctions.entrySet()) {
      String functionName = entrySet.getKey();
      Function function = entrySet.getValue();
      try {
        ScriptableObject.putProperty(scope, functionName, function);
      } catch (Exception e) {
        throw new StethoJsException(e, "Failed to setup function: %s", functionName);
      }
    }
  }

  private static class StethoJsException extends Exception {
    StethoJsException(Throwable rootCause, String format, Object...args) {
      super(args.length == 0 ? format : String.format(format, args), rootCause);
    }
  }
}

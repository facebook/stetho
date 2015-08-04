/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.rhino;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.facebook.stetho.inspector.console.RuntimeRepl;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

class JsRuntimeRepl implements RuntimeRepl {

  private final @NonNull ScriptableObject mJsScope;

  JsRuntimeRepl(@NonNull ScriptableObject scope) {
    mJsScope = scope;
  }

  @Override
  public @Nullable Object evaluate(@NonNull String expression) throws Throwable {
      Object result;
      final Context jsContext = enterJsContext();
      try {
        result = jsContext.evaluateString(mJsScope, expression, "chrome", 1, null);
      } finally {
        Context.exit();
      }

      return Context.jsToJava(result, Object.class);
  }

  @Override
  public boolean assignVariable(String varName, Object value) throws Throwable {
    enterJsContext();
    try {
      Object jsValue = Context.javaToJS(value, mJsScope);
      ScriptableObject.putProperty(mJsScope, varName, jsValue);
      return true;
    } finally {
      Context.exit();
    }
  }

  /**
   * Setups a proper javascript context so that it can run javascript code properly under android.
   * For android we need to disable bytecode generation since the android vms don't understand JVM bytecode.
   * @return a proper javascript context
   */
  static @NonNull Context enterJsContext() {
    final Context jsContext = Context.enter();

    // If we cause the context to throw a runtime exception from this point
    // we need to make sure that exit the context.
    try {
      jsContext.setLanguageVersion(Context.VERSION_1_8);

      // We can't let Rhino to optimize the JS and to use a JIT because it would generate JVM bytecode
      // and android runs on DEX bytecode. Instead we need to go in interpreted mode.
      jsContext.setOptimizationLevel(-1);
    } catch (RuntimeException e) {
      // Something bad happened to the javascript context but it might still be usable.
      // The first thing to do is to exit the context and then propagate the error.
      Context.exit();
      throw e;
    }

    return jsContext;
  }
}

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.rhino;

import com.facebook.stetho.inspector.console.CLog;
import com.facebook.stetho.inspector.protocol.module.Console.MessageLevel;
import com.facebook.stetho.inspector.protocol.module.Console.MessageSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

public class JsConsole extends ScriptableObject {

  /**
   * Serial version UID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * <p>The zero-parameter constructor.</p>
   *
   * <p>When Context.defineClass is called with this class, it will construct
   * JsConsole.prototype using this constructor.</p>
   */
  public JsConsole() {
    // Empty
  }

  public JsConsole(ScriptableObject scope) {
    setParentScope(scope);
    Object ctor = ScriptRuntime.getTopLevelProp(scope, "Console");
    if (ctor != null && ctor instanceof Scriptable) {
      Scriptable scriptable = (Scriptable) ctor;
      setPrototype((Scriptable) scriptable.get("prototype", scriptable));
    }
  }

  @Override
  public String getClassName() {
    return "Console";
  }

  @JSFunction
  public static void log(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    log(args);
  }

  private static void log(Object [] rawArgs) {
    String format = (String) rawArgs[0];

    String message;
    if (rawArgs.length == 1) {
      message = format;
    }
    else {
      // Using place holders in javascript (%d) causes a problem with java's String.format().
      // This happens because %d expects an int/Integer but in javascript numbers are floats.
      // For now as a best effort we just try to use jsToJava() to convert everything to objects.
      // This will need rework.
      Object [] args = new Object[rawArgs.length - 1];
      for (int i = 0; i < args.length; ++i) {
        Object arg = rawArgs[i + 1];
        arg = Context.jsToJava(arg, Object.class);
        args[i] = arg;
      }
      message = String.format(format, args);
    }

    CLog.writeToConsole(MessageLevel.LOG, MessageSource.JAVASCRIPT, message);
  }
}

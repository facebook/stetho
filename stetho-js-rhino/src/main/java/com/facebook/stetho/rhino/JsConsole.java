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

  // See https://developer.chrome.com/devtools/docs/console-api#consolelogobject-object
  private static void log(Object [] rawArgs) {
    String message = JsFormat.parse(rawArgs);
    CLog.writeToConsole(MessageLevel.LOG, MessageSource.JAVASCRIPT, message);
  }
}

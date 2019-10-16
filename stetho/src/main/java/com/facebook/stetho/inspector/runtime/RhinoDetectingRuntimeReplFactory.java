/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.runtime;

import android.content.Context;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.inspector.console.RuntimeRepl;
import com.facebook.stetho.inspector.console.RuntimeReplFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import androidx.annotation.Nullable;

/**
 * Attempts to locate stetho-js-rhino in the classpath and use it if available.  Otherwise falls
 * back to a no-op version which informs folks that they can include stetho-js-rhino for more
 * advanced functionality.
 * <p />
 * Eventually we should develop a kind of service locator somehow to make this more discoverable
 * and generalized.  For now with only one official implementation however it seems like overkill.
 */
public class RhinoDetectingRuntimeReplFactory implements RuntimeReplFactory {
  private final Context mContext;

  private boolean mSearchedForRhinoJs;
  private RuntimeReplFactory mRhinoReplFactory;
  private RuntimeException mRhinoJsUnexpectedError;

  public RhinoDetectingRuntimeReplFactory(Context context) {
    mContext = context;
  }

  @Override
  public RuntimeRepl newInstance() {
    if (!mSearchedForRhinoJs) {
      mSearchedForRhinoJs = true;
      try {
        mRhinoReplFactory = findRhinoReplFactory(mContext);
      } catch (RuntimeException e) {
        mRhinoJsUnexpectedError = e;
      }
    }
    if (mRhinoReplFactory != null) {
      return mRhinoReplFactory.newInstance();
    } else {
      return new RuntimeRepl() {
        @Override
        public Object evaluate(String expression) throws Exception {
          if (mRhinoJsUnexpectedError != null) {
            return "stetho-js-rhino threw: " + mRhinoJsUnexpectedError.toString();
          } else {
            return "Not supported without stetho-js-rhino dependency";
          }
        }
      };
    }
  }

  @Nullable
  private static RuntimeReplFactory findRhinoReplFactory(Context context) throws RuntimeException {
    try {
      Class<?> jsRuntimeReplFactory =
          Class.forName("com.facebook.stetho.rhino.JsRuntimeReplFactoryBuilder");
      Method defaultFactoryMethod =
          jsRuntimeReplFactory.getDeclaredMethod("defaultFactory", Context.class);
      return (RuntimeReplFactory) defaultFactoryMethod.invoke(null, context);
    } catch (ClassNotFoundException e) {
      LogUtil.i("Error finding stetho-js-rhino, cannot enable console evaluation!");
      return null;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}

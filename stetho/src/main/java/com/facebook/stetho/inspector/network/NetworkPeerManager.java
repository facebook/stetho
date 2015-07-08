/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.network;

import javax.annotation.Nullable;

import android.content.Context;

import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.PeersRegisteredListener;

public class NetworkPeerManager extends ChromePeerManager {
  private static NetworkPeerManager sInstance;

  private final ResponseBodyFileManager mResponseBodyFileManager;
  private final AsyncPrettyPrinterRegistry mAsyncPrettyPrinterRegistry;

  @Nullable
  public static synchronized NetworkPeerManager getInstanceOrNull() {
    return sInstance;
  }

  public static synchronized NetworkPeerManager getOrCreateInstance(Context context) {
    if (sInstance == null) {
      sInstance = new NetworkPeerManager(
          new ResponseBodyFileManager(
              context.getApplicationContext()));
    }
    return sInstance;
  }

  public NetworkPeerManager(ResponseBodyFileManager responseBodyFileManager) {
    mResponseBodyFileManager = responseBodyFileManager;
    setListener(mTempFileCleanup);
    mAsyncPrettyPrinterRegistry = new AsyncPrettyPrinterRegistry();
  }

  public ResponseBodyFileManager getResponseBodyFileManager() {
    return mResponseBodyFileManager;
  }

  public AsyncPrettyPrinterRegistry getAsyncPrettyPrinterRegistry() {
    return mAsyncPrettyPrinterRegistry;
  }

  private final PeersRegisteredListener mTempFileCleanup = new PeersRegisteredListener() {
    @Override
    protected void onFirstPeerRegistered() {
      mResponseBodyFileManager.cleanupFiles();
    }

    @Override
    protected void onLastPeerUnregistered() {
      mResponseBodyFileManager.cleanupFiles();
      AsyncPrettyPrinterExecutorHolder.sExecutorService.shutdownNow();
    }
  };
}

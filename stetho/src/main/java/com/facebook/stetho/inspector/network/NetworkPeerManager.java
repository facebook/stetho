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

  public static synchronized NetworkPeerManager getOrCreateInstance(
      Context context,
      AsyncPrettyPrinterRegistry prettyPrinterRegistry) {
    if (sInstance == null) {
      sInstance = new NetworkPeerManager(
          new ResponseBodyFileManager(
              context.getApplicationContext()),
          prettyPrinterRegistry);
    }
    return sInstance;
  }

  public NetworkPeerManager(
      ResponseBodyFileManager responseBodyFileManager,
      AsyncPrettyPrinterRegistry asyncPrettyPrinterRegistry) {
    mResponseBodyFileManager = responseBodyFileManager;
    setListener(mTempFileCleanup);
    mAsyncPrettyPrinterRegistry = asyncPrettyPrinterRegistry;
  }

  public ResponseBodyFileManager getResponseBodyFileManager() {
    return mResponseBodyFileManager;
  }

  private final PeersRegisteredListener mTempFileCleanup = new PeersRegisteredListener() {
    @Override
    protected void onFirstPeerRegistered() {
      mResponseBodyFileManager.cleanupFiles();
    }

    @Override
    protected void onLastPeerUnregistered() {
      mResponseBodyFileManager.cleanupFiles();
    }
  };
}

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
//
// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.server;

import java.io.IOException;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Credentials;
import android.net.LocalSocket;
import android.util.Log;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Utf8Charset;

import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public abstract class SecureHttpRequestHandler implements HttpRequestHandler {
  private static final Class<?> TAG = SecureHttpRequestHandler.class;
  private final Context mContext;

  public SecureHttpRequestHandler(Context context) {
    mContext = context;
  }

  @Override
  @SuppressLint("LogMethodNoExceptionInCatch")
  public final void handle(
      HttpRequest request,
      HttpResponse response,
      HttpContext context)
      throws HttpException, IOException {
    try {
      ensureSecureRequest(request, context);
      handleSecured(request, response, context);
    } catch (PeerAuthorizationException e) {
      LogUtil.e("Unauthorized request: " + e.getMessage());
      response.setStatusCode(HttpStatus.SC_FORBIDDEN);
      response.setEntity(new StringEntity(e.getMessage() + "\n", Utf8Charset.NAME));
    }
  }

  protected abstract void handleSecured(
      HttpRequest request,
      HttpResponse response,
      HttpContext context)
      throws HttpException, IOException;

  private void ensureSecureRequest(HttpRequest request, HttpContext context)
      throws PeerAuthorizationException, IOException {
    HttpConnection conn = (HttpConnection)context.getAttribute(ExecutionContext.HTTP_CONNECTION);
    if (!(conn instanceof LocalSocketHttpServerConnection)) {
      throw new PeerAuthorizationException("Unexpected connection class: " +
          conn.getClass().getName());
    }

    LocalSocketHttpServerConnection socketLikeConn = (LocalSocketHttpServerConnection)conn;
    LocalSocket socket = socketLikeConn.getSocket();

    enforcePermission(mContext, socket);
  }

  private static void enforcePermission(Context context, LocalSocket peer)
      throws IOException, PeerAuthorizationException {
    Credentials credentials = peer.getPeerCredentials();

    int uid = credentials.getUid();
    int pid = credentials.getPid();

    if (LogUtil.isLoggable(Log.VERBOSE)) {
      LogUtil.v("Got request from uid=%d, pid=%d", uid, pid);
    }

    String requiredPermission = Manifest.permission.DUMP;
    int checkResult = context.checkPermission(requiredPermission, pid, uid);
    if (checkResult != PackageManager.PERMISSION_GRANTED) {
      throw new PeerAuthorizationException(
          "Peer pid=" + pid + ", uid=" + uid + " does not have " + requiredPermission);
    }
  }
}

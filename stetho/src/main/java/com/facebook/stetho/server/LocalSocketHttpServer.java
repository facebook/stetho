// Copyright 2004-present Facebook. All Rights Reserved.
// This is based on ElementalHttpServer.java in the Apache httpcore
// examples.

/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.facebook.stetho.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.ProcessUtil;
import com.facebook.stetho.common.Util;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

public class LocalSocketHttpServer {

  private static final String WORKDER_THREAD_NAME_PREFIX = "StethoWorker";
  private static final int MAX_BIND_RETRIES = 3;
  private static final int TIME_BETWEEN_BIND_RETRIES_MS = 1000;
  private static final String SOCKET_NAME_PREFIX = "stetho_";

  /**
   * Convince {@code chrome://inspect/devices} that we're "one of them" :)
   */
  private static final String SOCKET_NAME_SUFFIX = "_devtools_remote";

  private static final AtomicInteger sThreadId = new AtomicInteger();

  private final RegistryInitializer mRegistryInitializer;
  private final String mAddress;
  private Thread mListenerThread;
  private boolean mStopped;
  private LocalServerSocket mServerSocket;

  /**
   * @param registryInitializer lazy initializer for the {@link HttpRequestHandlerRegistry}.
   *     This is only initialized after the first socket has connected, and this determines
   *     what handlers this server uses to process requests.
   */
  public LocalSocketHttpServer(RegistryInitializer registryInitializer) {
    this(registryInitializer, null /* address */);
  }

  /**
   * @param registryInitializer lazy initializer for the {@link HttpRequestHandlerRegistry}.
   *     This is only initialized after the first socket has connected, and this determines
   *     what handlers this server uses to process requests.
   * @param address the local socket address to listen on.
   */
  public LocalSocketHttpServer(RegistryInitializer registryInitializer, String address) {
    mRegistryInitializer = Util.throwIfNull(registryInitializer);
    mAddress = address;
  }

  /**
   * Binds to the address and listens for connections.
   * <p/>
   * If successful, this thread blocks forever or until {@link #stop} is called, whichever
   * happens first.
   *
   * @throws IOException if address is not specified and we cannot read the process name from
   *   /proc/self/cmdline.
   */
  public void run() throws IOException {
    synchronized (this) {
      if (mStopped) {
        return;
      }
      mListenerThread = Thread.currentThread();
    }

    String address = (mAddress != null) ? mAddress : getDefaultAddress();
    listenOnAddress(address);
  }

  private void listenOnAddress(String address) {
    bindToSocket(address);

    if (mServerSocket == null) {
      return;
    }

    HttpParams params = null;
    HttpService service = null;

    while (!Thread.interrupted()) {
      LocalSocketHttpServerConnection connection = new LocalSocketHttpServerConnection();
      try {
        // Use previously accepted socket the first time around, otherwise wait to
        // accept another.
        LocalSocket socket = mServerSocket.accept();

        if (params == null) {
          params = createParams();
        }
        if (service == null) {
          service = createService(params);
        }
        connection.bind(socket, params);

        // Start worker thread
        Thread t = new WorkerThread(service, connection);
        t.setDaemon(true);
        t.start();
      } catch (SocketException se) {
        // ignore exception if interrupting the thread
        if (!Thread.interrupted()) {
          LogUtil.w(se, "I/O error");
        }
      } catch (InterruptedIOException ex) {
        break;
      } catch (IOException e) {
        LogUtil.w(e, "I/O error initialising connection thread");
        break;
      }
    }
  }

  private static String getDefaultAddress() throws IOException {
    return
        SOCKET_NAME_PREFIX +
        tidyProcessName(ProcessUtil.getProcessName()) +
        SOCKET_NAME_SUFFIX;
  }

  private static String tidyProcessName(String processName) {
    return processName.replaceAll("[\\\\\\.:]", "_");
  }

  private HttpParams createParams() {
    return new BasicHttpParams()
        .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
        .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
        .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
        .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
        .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "Stetho")
        .setParameter(CoreProtocolPNames.PROTOCOL_VERSION, "HTTP/1.1");
  }

  private HttpService createService(HttpParams params) {
    HttpRequestHandlerRegistry registry = mRegistryInitializer.getRegistry();

    BasicHttpProcessor httpproc = new BasicHttpProcessor();
    httpproc.addInterceptor(new ResponseDate());
    httpproc.addInterceptor(new ResponseServer());
    httpproc.addInterceptor(new ResponseContent());
    httpproc.addInterceptor(new ResponseConnControl());

    HttpService service = new HttpService(
        httpproc,
        new DefaultConnectionReuseStrategy(),
        new DefaultHttpResponseFactory());
    service.setParams(params);
    service.setHandlerResolver(registry);

    return service;
  }

  /**
   * Stops the listener thread and unbinds the address.
   */
  public void stop() {
    synchronized (this) {
      mStopped = true;
      if (mListenerThread == null) {
        return;
      }
    }

    mListenerThread.interrupt();
    try {
      if (mServerSocket != null) {
        mServerSocket.close();
      }
    } catch (IOException e) {}
  }

  private void bindToSocket(String address) {
    try {
      int retries = MAX_BIND_RETRIES;
      while (retries > 0) {
        retries--;
        try {
          if (LogUtil.isLoggable(Log.DEBUG)) {
            LogUtil.d("Binding server to " + address);
          }
          mServerSocket = new LocalServerSocket(address);
          LogUtil.i("Listening on @" + address);
          return;
        } catch (BindException be) {
          LogUtil.w(be, "Binding error, sleep 1 second ...");
          if (retries == 0)
            throw be;
          Thread.sleep(TIME_BETWEEN_BIND_RETRIES_MS);
        }
      }
    } catch (Exception e) {
      LogUtil.e(e, "Could not bind to socket.");
    }
  }

  private static class WorkerThread extends Thread {

    private final HttpService httpservice;
    private final HttpServerConnection conn;

    public WorkerThread(
        final HttpService httpservice,
        final HttpServerConnection conn) {
      super(WORKDER_THREAD_NAME_PREFIX + sThreadId.incrementAndGet());
      this.httpservice = httpservice;
      this.conn = conn;
    }

    @Override
    @SuppressLint("LogMethodNoExceptionInCatch")
    public void run() {
      HttpContext context = new BasicHttpContext(null);
      try {
        if (!Thread.interrupted() && conn.isOpen()) {
          httpservice.handleRequest(conn, context);
        }
      } catch (ConnectionClosedException ex) {
        LogUtil.w("Client closed connection: %s", ex);
      } catch (IOException ex) {
        LogUtil.w("I/O error: %s", ex);
      } catch (HttpException ex) {
        LogUtil.w("Unrecoverable HTTP protocol violation: %s", ex);
      } finally {
        try {
          conn.close();
        } catch (IOException ignore) {
        }
      }
    }
  }
}

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.server;

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
import org.apache.http.protocol.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalSocketHttpServer {

  private static final String WORKDER_THREAD_NAME_PREFIX = "StethoWorker";
  private static final int MAX_BIND_RETRIES = 2;
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
   * @throws IOException Thrown on failure to bind the socket.
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

  private void listenOnAddress(String address) throws IOException {
    mServerSocket = bindToSocket(address);
    LogUtil.i("Listening on @" + address);

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
        ProcessUtil.getProcessName() +
        SOCKET_NAME_SUFFIX;
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

  @Nonnull
  private static LocalServerSocket bindToSocket(String address) throws IOException {
    int retries = MAX_BIND_RETRIES;
    IOException firstException = null;
    do {
      try {
        if (LogUtil.isLoggable(Log.DEBUG)) {
          LogUtil.d("Trying to bind to @" + address);
        }
        return new LocalServerSocket(address);
      } catch (BindException be) {
        LogUtil.w(be, "Binding error, sleep " + TIME_BETWEEN_BIND_RETRIES_MS + " ms...");
        if (firstException == null) {
          firstException = be;
        }
        Util.sleepUninterruptibly(TIME_BETWEEN_BIND_RETRIES_MS);
      }
    } while (retries-- > 0);

    throw firstException;
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

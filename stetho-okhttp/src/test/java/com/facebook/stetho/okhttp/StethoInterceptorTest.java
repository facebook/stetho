/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.okhttp;

import android.net.Uri;
import android.os.Build;
import com.facebook.stetho.inspector.network.DecompressionHelper;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.ResponseHandler;
import com.squareup.okhttp.*;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;

@Config(emulateSdk = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "javax.net.ssl.*" })
@PrepareForTest(NetworkEventReporterImpl.class)
public class StethoInterceptorTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Test
  public void testHappyPath() throws IOException {
    PowerMockito.mockStatic(NetworkEventReporterImpl.class);

    final NetworkEventReporter mockEventReporter = Mockito.mock(NetworkEventReporter.class);
    InOrder inOrder = Mockito.inOrder(mockEventReporter);
    Mockito.when(mockEventReporter.isEnabled()).thenReturn(true);
    ByteArrayOutputStream capturedOutput = hookAlmostRealInterpretResponseStream(mockEventReporter);
    PowerMockito.when(NetworkEventReporterImpl.get()).thenReturn(mockEventReporter);

    StethoInterceptor interceptor = new StethoInterceptor();

    Uri requestUri = Uri.parse("http://www.facebook.com/nowhere");
    Request request = new Request.Builder()
        .url(requestUri.toString())
        .method(
            "POST",
            RequestBody.create(MediaType.parse("text/plain"), "Test input"))
        .build();
    String originalBodyData = "Success!";
    Response reply = new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .body(ResponseBody.create(MediaType.parse("text/plain"), originalBodyData))
        .build();
    Response filteredResponse =
        interceptor.intercept(
            new SimpleTestChain(request, reply, null));

    inOrder.verify(mockEventReporter).isEnabled();
    inOrder.verify(mockEventReporter)
        .requestWillBeSent(any(NetworkEventReporter.InspectorRequest.class));
    inOrder.verify(mockEventReporter).dataSent(anyString(), anyInt(), anyInt());
    inOrder.verify(mockEventReporter)
        .responseHeadersReceived(any(NetworkEventReporter.InspectorResponse.class));

    String filteredResponseString = filteredResponse.body().string();
    String interceptedOutput = capturedOutput.toString();

    inOrder.verify(mockEventReporter).dataReceived(anyString(), anyInt(), anyInt());
    inOrder.verify(mockEventReporter).responseReadFinished(anyString());

    assertEquals(originalBodyData, filteredResponseString);
    assertEquals(originalBodyData, interceptedOutput);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testWithCompression() throws IOException {
    PowerMockito.mockStatic(NetworkEventReporterImpl.class);

    final NetworkEventReporter mockEventReporter = Mockito.mock(NetworkEventReporter.class);
    Mockito.when(mockEventReporter.isEnabled()).thenReturn(true);
    ByteArrayOutputStream capturedOutput = hookAlmostRealInterpretResponseStream(mockEventReporter);
    PowerMockito.when(NetworkEventReporterImpl.get()).thenReturn(mockEventReporter);

    byte[] uncompressedData = repeat(".", 1024).getBytes();
    byte[] compressedData = compress(uncompressedData);

    MockWebServer server = new MockWebServer();
    server.play();
    server.enqueue(new MockResponse()
        .setBody(compressedData)
        .addHeader("Content-Encoding: gzip"));

    OkHttpClient client = new OkHttpClient();
    client.networkInterceptors().add(new StethoInterceptor());

    Request request = new Request.Builder()
        .url(server.getUrl("/"))
        .build();
    Response response = client.newCall(request).execute();

    // Verify that the final output and the caller both saw the uncompressed stream.
    assertArrayEquals(uncompressedData, response.body().bytes());
    assertArrayEquals(uncompressedData, capturedOutput.toByteArray());

    // And verify that the StethoInterceptor was able to see both.
    Mockito.verify(mockEventReporter)
        .dataReceived(
            anyString(),
            eq(compressedData.length),
            eq(uncompressedData.length));

    server.shutdown();
  }

  private static String repeat(String s, int reps) {
    StringBuilder b = new StringBuilder(s.length() * reps);
    while (reps-- > 0) {
      b.append(s);
    }
    return b.toString();
  }

  private static byte[] compress(byte[] data) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    GZIPOutputStream out = new GZIPOutputStream(buf);
    out.write(data);
    out.close();
    return buf.toByteArray();
  }

  /**
   * Provide a suitably "real" implementation of
   * {@link NetworkEventReporter#interpretResponseStream} for our mock to test that
   * events are properly delegated.
   */
  private static ByteArrayOutputStream hookAlmostRealInterpretResponseStream(
      final NetworkEventReporter mockEventReporter) {
    final ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
    Mockito.when(
        mockEventReporter.interpretResponseStream(
            anyString(),
            anyString(),
            anyString(),
            any(InputStream.class),
            any(ResponseHandler.class)))
        .thenAnswer(
            new Answer<InputStream>() {
              @Override
              public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                String requestId = (String)args[0];
                String contentEncoding = (String)args[2];
                InputStream responseStream = (InputStream)args[3];
                ResponseHandler responseHandler = (ResponseHandler)args[4];
                return DecompressionHelper.teeInputWithDecompression(
                    null /* networkPeerManager */,
                    requestId,
                    responseStream,
                    capturedOutput,
                    contentEncoding,
                    responseHandler);
              }
            });
    return capturedOutput;
  }

  private static class SimpleTestChain implements Interceptor.Chain {
    private final Request mRequest;
    private final Response mResponse;
    @Nullable private final Connection mConnection;

    public SimpleTestChain(Request request, Response response, @Nullable Connection connection) {
      mRequest = request;
      mResponse = response;
      mConnection = connection;
    }

    @Override
    public Request request() {
      return mRequest;
    }

    @Override
    public Response proceed(Request request) throws IOException {
      if (mRequest != request) {
        throw new IllegalArgumentException(
            "Expected " + System.identityHashCode(mRequest) +
                "; got " + System.identityHashCode(request));
      }
      return mResponse;
    }

    @Override
    public Connection connection() {
      return mConnection;
    }
  }
}

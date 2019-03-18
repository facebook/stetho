/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.okhttp3;

import android.net.Uri;
import android.os.Build;
import com.facebook.stetho.inspector.network.DecompressionHelper;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.ResponseHandler;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.Before;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;

@Config(emulateSdk = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "javax.net.ssl.*" })
@PrepareForTest(NetworkEventReporterImpl.class)
public class StethoInterceptorTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private NetworkEventReporter mMockEventReporter;
  private StethoInterceptor mInterceptor;
  private OkHttpClient mClientWithInterceptor;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(NetworkEventReporterImpl.class);

    mMockEventReporter = mock(NetworkEventReporter.class);
    Mockito.when(mMockEventReporter.isEnabled()).thenReturn(true);
    PowerMockito.when(NetworkEventReporterImpl.get()).thenReturn(mMockEventReporter);

    mInterceptor = new StethoInterceptor();
    mClientWithInterceptor = new OkHttpClient.Builder()
            .addNetworkInterceptor(mInterceptor)
            .build();
  }

  @Test
  public void testHappyPath() throws IOException {
    InOrder inOrder = Mockito.inOrder(mMockEventReporter);
    hookAlmostRealRequestWillBeSent(mMockEventReporter);
    ByteArrayOutputStream capturedOutput =
        hookAlmostRealInterpretResponseStream(mMockEventReporter);

    Uri requestUri = Uri.parse("http://www.facebook.com/nowhere");
    String requestText = "Test input";
    Request request = new Request.Builder()
        .url(requestUri.toString())
        .method(
            "POST",
            RequestBody.create(MediaType.parse("text/plain"), requestText))
        .build();
    String originalBodyData = "Success!";
    Response reply = new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .body(ResponseBody.create(MediaType.parse("text/plain"), originalBodyData))
        .build();
    Response filteredResponse =
        mInterceptor.intercept(
            new SimpleTestChain(request, reply, mock(Connection.class)));

    inOrder.verify(mMockEventReporter).isEnabled();
    inOrder.verify(mMockEventReporter)
        .requestWillBeSent(any(NetworkEventReporter.InspectorRequest.class));
    inOrder.verify(mMockEventReporter)
        .dataSent(
            anyString(),
            eq(requestText.length()),
            eq(requestText.length()));
    inOrder.verify(mMockEventReporter)
        .responseHeadersReceived(any(NetworkEventReporter.InspectorResponse.class));

    String filteredResponseString = filteredResponse.body().string();
    String interceptedOutput = capturedOutput.toString();

    inOrder.verify(mMockEventReporter).dataReceived(anyString(), anyInt(), anyInt());
    inOrder.verify(mMockEventReporter).responseReadFinished(anyString());

    assertEquals(originalBodyData, filteredResponseString);
    assertEquals(originalBodyData, interceptedOutput);

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testWithRequestCompression() throws IOException {
    AtomicReference<NetworkEventReporter.InspectorRequest> capturedRequest =
        hookAlmostRealRequestWillBeSent(mMockEventReporter);

    MockWebServer server = new MockWebServer();
    server.start();
    server.enqueue(new MockResponse()
        .setBody("Success!"));

    final byte[] decompressed = "Request text".getBytes();
    final byte[] compressed = compress(decompressed);
    assertNotEquals(
        "Bogus test: decompressed and compressed lengths match",
        compressed.length, decompressed.length);

    RequestBody compressedBody = RequestBody.create(
        MediaType.parse("text/plain"),
        compress(decompressed));
    Request request = new Request.Builder()
        .url(server.url("/"))
        .addHeader("Content-Encoding", "gzip")
        .post(compressedBody)
        .build();
    Response response = mClientWithInterceptor.newCall(request).execute();

    // Force a read to complete the flow.
    response.body().string();

    assertArrayEquals(decompressed, capturedRequest.get().body());
    Mockito.verify(mMockEventReporter)
        .dataSent(
            anyString(),
            eq(decompressed.length),
            eq(compressed.length));

    server.shutdown();
  }

  @Test
  public void testWithResponseCompression() throws IOException {
    ByteArrayOutputStream capturedOutput = hookAlmostRealInterpretResponseStream(mMockEventReporter);

    byte[] uncompressedData = repeat(".", 1024).getBytes();
    byte[] compressedData = compress(uncompressedData);

    MockWebServer server = new MockWebServer();
    server.start();
    server.enqueue(new MockResponse()
        .setBody(new Buffer().write(compressedData))
        .addHeader("Content-Encoding: gzip"));

    Request request = new Request.Builder()
        .url(server.url("/"))
        .build();
    Response response = mClientWithInterceptor.newCall(request).execute();

    // Verify that the final output and the caller both saw the uncompressed stream.
    assertArrayEquals(uncompressedData, response.body().bytes());
    assertArrayEquals(uncompressedData, capturedOutput.toByteArray());

    // And verify that the StethoInterceptor was able to see both.
    Mockito.verify(mMockEventReporter)
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

  private static AtomicReference<NetworkEventReporter.InspectorRequest>
      hookAlmostRealRequestWillBeSent(
          final NetworkEventReporter mockEventReporter) {
    final AtomicReference<NetworkEventReporter.InspectorRequest> capturedRequest =
        new AtomicReference<>(null);
    Mockito.doAnswer(
        new Answer<Void>() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            NetworkEventReporter.InspectorRequest request =
                (NetworkEventReporter.InspectorRequest)args[0];
            capturedRequest.set(request);

            // Access the body, causing the body helper to perform decompression...
            request.body();
            return null;
          }
        })
        .when(mockEventReporter)
            .requestWillBeSent(
                any(NetworkEventReporter.InspectorRequest.class));
    return capturedRequest;
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

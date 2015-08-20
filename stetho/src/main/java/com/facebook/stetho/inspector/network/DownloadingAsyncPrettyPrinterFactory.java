package com.facebook.stetho.inspector.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.facebook.stetho.common.ExceptionUtil;
import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;

/**
 * Abstract class for pretty printer factory that asynchronously downloads schema needed for
 * pretty printing the payload
 */
public abstract class DownloadingAsyncPrettyPrinterFactory implements AsyncPrettyPrinterFactory {

  @Override
  public AsyncPrettyPrinter getInstance(final String headerName, final String headerValue) {

    final MatchResult result = matchAndParseHeader(headerName, headerValue);
    if (result == null) {
      return null;
    }
    String uri = result.getSchemaUri();
    final URL schemaURL = parseURL(uri);
    if (schemaURL == null) {
      String errorMessage = "[Failed to parse header: "
          + headerName + " : " + headerValue + " ]";
      return getErrorAsyncPrettyPrinter(errorMessage);
    } else {
      final String schemaCache;
      AsyncPrettyPrinterSchemaManager schemaFileManager
          = AsyncPrettyPrinterSchemaManager.getInstance();
      schemaCache = schemaFileManager.get(schemaURL);
      if (schemaCache != null) {
        return new AsyncPrettyPrinter() {
          @Override
          public void printTo(PrintWriter output, InputStream payload) throws IOException {
            doPrint(output, payload, schemaCache);
          }

          @Override
          public PrettyPrinterDisplayType getPrettifiedType() {
            return result.getDisplayType();
          }
        };
      } else {
        ExecutorService  executorService = AsyncPrettyPrinterExecutorHolder.getExecutorService();
        if (executorService == null) {
          //last peer is unregistered...
          return null;
        }
        final Future<String> response = executorService.submit(new Request(schemaURL));
        return new AsyncPrettyPrinter() {

          @Override
          public void printTo(PrintWriter output, InputStream payload)
              throws IOException {
            try {
              String schema;
              try {
                schema = response.get();
              } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (IOException.class.isInstance(cause) ||
                    IllegalArgumentException.class.isInstance(cause)) {
                  doErrorPrint(
                      output,
                      payload,
                      "Cannot successfully download schema: "
                          + e.getMessage());
                  return;
                } else {
                  throw e;
                }
              }
              doPrint(output, payload, schema);
            } catch (InterruptedException e) {
              doErrorPrint(
                  output,
                  payload,
                  "Encountered spurious interrupt while downloading schema for pretty printing: "
                      + e.getMessage());
            } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              throw ExceptionUtil.propagate(cause);
            }
          }

          @Override
          public PrettyPrinterDisplayType getPrettifiedType() {
            return result.getDisplayType();
          }
        };
      }
    }
  }

  /**
   * Match the correct header that contains information about the schema uri
   * @param headerName header name of a response that needs to be pretty printed
   * @param headerValue header value which contains the URI for
   * the schema data needed to pretty print the response body
   * @return MatchResult that has the schema uri and the type of prettified result. Null
   * if there is no correct header match.
   */
  @Nullable
  protected abstract MatchResult matchAndParseHeader(String headerName, String headerValue);

  /**
   * Note that the IOException thrown by this method will be propagated all the way up and
   * yield an error to the chrome devtools
   */
  protected abstract void doPrint(PrintWriter output, InputStream payload, String schema)
      throws IOException;

  @Nullable
  private static URL parseURL(String uri) {
    try {
      return new URL(uri);
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private static void doErrorPrint(PrintWriter output, InputStream payload, String errorMessage)
      throws IOException {
    output.print(errorMessage + "\n" + Util.readAsUTF8(payload));
  }

  private static AsyncPrettyPrinter getErrorAsyncPrettyPrinter(
      final String errorMessage) {
    return new AsyncPrettyPrinter() {
      @Override
      public void printTo(PrintWriter output, InputStream payload) throws IOException {
        doErrorPrint(output, payload, errorMessage);
      }

      @Override
      public PrettyPrinterDisplayType getPrettifiedType() {
        return PrettyPrinterDisplayType.TEXT;
      }
    };
  }

  protected class MatchResult {
    private final String mSchemaUri;
    private final PrettyPrinterDisplayType mDisplayType;

    public MatchResult(String schemaUri, PrettyPrinterDisplayType displayType) {
      mSchemaUri = schemaUri;
      mDisplayType = displayType;
    }

    public String getSchemaUri() {
      return mSchemaUri;
    }

    public PrettyPrinterDisplayType getDisplayType() {
      return mDisplayType;
    }
  }

  private static class Request implements Callable<String> {
    private final URL url;

    public Request(URL url) {
      this.url = url;
    }

    @Nullable
    @Override
    public String call() throws IOException {
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      int statusCode = connection.getResponseCode();
      if (statusCode != 200) {
        throw new IOException("Got status code: " + statusCode + " while downloading " +
            "schema with url: " + url.toString());
      }
      InputStream urlStream = connection.getInputStream();
      try {
        AsyncPrettyPrinterSchemaManager schemaFileManager
            = AsyncPrettyPrinterSchemaManager.getInstance();
        String result = Util.readAsUTF8(urlStream);
        schemaFileManager.put(url, result);
        return result;
      } finally {
        urlStream.close();
      }
    }
  }
}


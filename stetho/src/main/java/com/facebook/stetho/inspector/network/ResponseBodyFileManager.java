// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.network;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Base64;
import android.util.Base64OutputStream;

import com.facebook.stetho.common.LogRedirector;
import com.facebook.stetho.common.Util;

/**
 * Manages temporary files created by {@link ChromeHttpFlowObserver} to serve request bodies.
 */
public class ResponseBodyFileManager {
  private static final String TAG = "ResponseBodyFileManager";
  private static final String FILENAME_PREFIX = "network-response-body-";

  private final Context mContext;

  public ResponseBodyFileManager(Context context) {
    mContext = context;
  }

  public void cleanupFiles() {
    for (File file : mContext.getFilesDir().listFiles()) {
      if (file.getName().startsWith(FILENAME_PREFIX)) {
        if (!file.delete()) {
          LogRedirector.w(TAG, "Failed to delete " + file.getAbsolutePath());
        }
      }
    }
    LogRedirector.i(TAG, "Cleaned up temporary network files.");
  }

  public ResponseBodyData readFile(String requestId) throws IOException {
    InputStream in = mContext.openFileInput(getFilename(requestId));
    try {
      int firstByte = in.read();
      if (firstByte == -1) {
        throw new EOFException("Failed to read base64Encode byte");
      }
      ResponseBodyData bodyData = new ResponseBodyData();
      bodyData.base64Encoded = firstByte != 0;
      bodyData.data = readContentsAsUTF8(in);
      return bodyData;
    } finally {
      in.close();
    }
  }

  private static String readContentsAsUTF8(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Util.copy(in, out, new byte[1024]);
    return out.toString("UTF-8");
  }

  public OutputStream openResponseBodyFile(String requestId, boolean base64Encode)
      throws IOException {
    OutputStream out = mContext.openFileOutput(getFilename(requestId), Context.MODE_PRIVATE);
    out.write(base64Encode ? 1 : 0);
    if (base64Encode) {
      return new Base64OutputStream(out, Base64.DEFAULT);
    } else {
      return out;
    }
  }

  private static String getFilename(String requestId) {
    return FILENAME_PREFIX + requestId;
  }
}

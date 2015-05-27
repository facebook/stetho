package com.facebook.stetho.heap;

import android.os.Debug;
import android.os.Environment;
import android.os.SystemClock;
import com.facebook.stetho.common.LogUtil;
import org.apache.harmony.dalvik.ddmc.DdmVmInternal;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instrumented allocation tracker similar to {@link Debug#startAllocCounting()} but
 * with the ability to inspect specific allocations.  Multiple file formats are supported,
 * including one which loads in the Chrome Developer Tools UI.
 *
 * @see #start(String)
 * @see #stop()
 */
public class AllocationTracker {
  public static final int FORMAT_DDMS = 2;
  public static final int FORMAT_GRIMSEY = 3;

  @Nullable
  private static volatile TrackingInfo sTrackingInfo;

  private static final AtomicInteger sProfileCount = new AtomicInteger(0);

  /**
   * Begin recording allocations, storing the results in {@code nameOrPath}.  File format is
   * determined automatically by suffix:
   * <ul>
   * <li>{@code .allocs} - DDMS' internal format, visualized by the Android Monitor tool</li>
   * <li>{@code .grimsey} - Stetho's custom format, visualized by
   * {@code ./scripts/allocviewer.py}</li>
   * </ul>
   * <p />
   * Defaults to the Grimsey file format if none of the above are specified.
   *
   * @param nameOrPath Base name of the file or absolute path.  If basename is provided it will
   *     be adjusted if necessary to store into
   *     {@link Environment#getExternalStorageDirectory()}.
   *
   * @throws IllegalStateException Allocation tracking is already started.
   */
  public static void start(String nameOrPath) {
    start(nameOrPath, determineFileFormat(nameOrPath));
  }

  private static int determineFileFormat(String nameOrPath) {
    String suffix = getFilenameSuffix(nameOrPath);
    switch (suffix) {
      case ".allocs": return FORMAT_DDMS;
      case ".grimsey":
      default: return FORMAT_GRIMSEY;
    }
  }

  @Nullable
  private static String getFilenameSuffix(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex >= 0) {
      return filename.substring(dotIndex, filename.length());
    }
    return null;
  }

  /**
   * @see #start(String)
   */
  public static void start(String nameOrPath, int fileFormat) {
    signalStart(determineStoragePath(nameOrPath).getPath(), fileFormat);
    DdmVmInternal.enableRecentAllocations(true);
    Debug.startAllocCounting();
  }

  private static File determineStoragePath(String filename) {
    File file = new File(filename);
    if (file.isAbsolute()) {
      return file;
    }
    return new File(Environment.getExternalStorageDirectory(), filename);
  }

  /**
   * Stop recording allocations and write the result to the file specified in
   * {@link #start(String)}.
   * <p/>
   * This method may have significant overhead, possibly suspending the VM for a long period of
   * time if there have been a large number of allocations.
   */
  public static void stop() {
    Debug.stopAllocCounting();
    int totalAllocCount = Debug.getGlobalAllocCount();
    byte[] rawData = DdmVmInternal.getRecentAllocations();
    DdmVmInternal.enableRecentAllocations(false);
    saveDataQuietly(rawData, sProfileCount.incrementAndGet());
    signalStop();
    int capturedAllocCount = fastParseAllocationCount(rawData);
    if (capturedAllocCount < totalAllocCount) {
      LogUtil.e("Allocation tracking overrun: allocated " + totalAllocCount +
          " (max " + capturedAllocCount + ")");
    }
    LogUtil.i("Processed " + capturedAllocCount + " allocations");
  }

  /**
   * Quickly parse out the number of allocation entries.
   *
   * @see DdmAllocationParser
   */
  private static int fastParseAllocationCount(byte[] rawData) {
    ByteBuffer buf = ByteBuffer.wrap(rawData);
    return buf.getShort(3);
  }

  private static void saveDataQuietly(
      byte[] rawData,
      int snapshotNum) {
    long startTimeMs = SystemClock.elapsedRealtime();
    final TrackingInfo trackingInfo = sTrackingInfo;
    try {
      switch (trackingInfo.format) {
        case FORMAT_DDMS:
          saveRawData(trackingInfo.filename, rawData);
          break;
        case FORMAT_GRIMSEY:
          saveGrimsey(trackingInfo.filename, rawData);
          break;
        default:
          throw new UnsupportedOperationException("Unknown format " + trackingInfo.format);
      }
      long elapsed = SystemClock.elapsedRealtime() - startTimeMs;
      LogUtil.i("Wrote " + trackingInfo.filename + " in " + elapsed + " ms");
    } catch (IOException e) {
      LogUtil.e(e,
          "Failed to write %s (do you have WRITE_EXTERNAL_STORAGE permission?)",
          trackingInfo.filename);
    }
  }

  /**
   * Determine if {@link #start} has been called without a matching {@link #stop}.
   */
  public static boolean isStarted() {
    return sTrackingInfo != null;
  }

  private static synchronized void signalStart(String filename, int fileFormat) {
    if (sTrackingInfo != null) {
      throw new IllegalStateException("Concurrent tracing not allowed");
    }
    sTrackingInfo = new TrackingInfo(filename, fileFormat);
  }

  private static void signalStop() {
    sTrackingInfo = null;
  }

  private static void saveRawData(String file, byte[] rawData) throws IOException {
    FileOutputStream out = new FileOutputStream(file);
    try {
      out.write(rawData);
    } finally {
      out.close();
    }
  }

  private static void saveGrimsey(
      String filename,
      byte[] rawData) throws IOException {
    GrimseyConverter writer = new GrimseyConverter(new FileOutputStream(filename));
    try {
      writer.writeDDMSData(rawData);
    } finally {
      writer.close();
    }
  }

  private static class TrackingInfo {
    public final String filename;
    public final int format;

    public TrackingInfo(String filename, int format) {
      this.filename = filename;
      this.format = format;
    }
  }
}

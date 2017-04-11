package com.facebook.stetho.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;
import com.facebook.stetho.heap.AllocationTracker;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfilerActivity extends Activity {
  private static final String TAG = "ProfilerActivity";

  private CheckBox mMicroscopedProfile;

  public static void show(Context context) {
    context.startActivity(new Intent(context, ProfilerActivity.class));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.profiler_activity);

    mMicroscopedProfile = (CheckBox)findViewById(R.id.microscoped_profile);
    findViewById(R.id.alloc_simple_btn).setOnClickListener(mProfileButtonClicked);
    findViewById(R.id.alloc_gotcha_btn).setOnClickListener(mProfileButtonClicked);
  }

  private final View.OnClickListener mProfileButtonClicked = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      boolean microscoped = mMicroscopedProfile.isChecked();
      File microscopedPath = null;
      if (microscoped) {
        if (AllocationTracker.isStarted()) {
          toast(getString(R.string.already_profiling));
          return;
        }
        microscopedPath = new File(
            Environment.getExternalStorageDirectory(),
            "allocations.grimsey");
        AllocationTracker.start(microscopedPath.getAbsolutePath());
      }
      try {
        handleClick(v.getId());
      } finally {
        if (microscoped) {
          AllocationTracker.stop();
          toast("Wrote " + microscopedPath);
        }
      }
    }

    private void handleClick(int id) {
      if (id == R.id.alloc_simple_btn) {
        doAllocSimple();
      } else if (id == R.id.alloc_gotcha_btn) {
        doAllocGotcha();
      }
    }
  };

  private void toast(CharSequence text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }

  /**
   * Simple demonstration of commonplace allocations in Java applications.  Allocates
   * approximately 300 objects, easily identifiable.
   */
  private static ArrayList<String> doAllocSimple() {
    int stringListSize = 100;

    // ArrayList and Object[]
    ArrayList<String> stringList = new ArrayList<String>(stringListSize);

    for (int i = 0; i < stringListSize; i++) {
      // StringBuilder, char[], String.
      String s = "String #" + i;
      stringList.add(s);
    }

    return stringList;
  }

  /**
   * Mock creation of a legacy form-encoded HTTP request body, demonstrating the serious
   * gotchas that lurk within the Java runtime implementation.  Allocates a surprising 4000+
   * objects, at a total size of 200KB+.
   * <p />
   * PSA: Do not use form-encoding.  Let Stetho explain why... :)
   */
  private static byte[] doAllocGotcha() {
    int parameterMultiplier = 100;

    // HashMap, Hashtable, Map.Entry[] (which will be reallocated/copied 4 or 5 times)
    HashMap<String, Object> mQueryParameters = new HashMap<String, Object>();

    // String[], and on first run the string literals themselves.
    String[] parameterPrefixes = { "foo", "bar", "baz" };
    for (int i = 0; i < parameterMultiplier; i++) {
      String prefix = parameterPrefixes[i % parameterPrefixes.length];

      // 2x StringBuilder, char[], String
      String integerKey = prefix + "-int-" + i;
      String stringKey = prefix + "-str-" + i;

      // Map.Entry, Integer (for autoboxing)
      mQueryParameters.put(integerKey, i + 1000);

      // Map.Entry (but just wait until we URLEncode...)
      mQueryParameters.put(stringKey, "!@#$&%*# ");
    }

    // StringBuilder, char[]
    StringBuilder requestBuilder = new StringBuilder();

    // EntrySet, Iterator<Map.Entry<>>
    for (Map.Entry<String, Object> entry : mQueryParameters.entrySet()) {
      // String (for Integer case only)
      String valueString = entry.getValue().toString();

      // Huge allocation bomb; see method for allocations...
      formEncodePair(requestBuilder, entry.getKey(), valueString);
    }

    // String only, no copy
    String request = requestBuilder.toString();

    // byte[]
    byte[] requestEncoded = request.getBytes();

    return requestEncoded;
  }

  private static void formEncodePair(StringBuilder request, String key, String value) {
    try {
      if (request.length() > 0) {
        request.append('&');
      }
      // Allocates many String/char[] pairs for each encoded segment.
      request.append(URLEncoder.encode(key, "UTF-8"));
      request.append('=');
      request.append(URLEncoder.encode(value, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}

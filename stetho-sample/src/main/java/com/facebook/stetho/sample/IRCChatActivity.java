/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class IRCChatActivity extends Activity {
  private static final int DEFAULT_PORT = 6667;

  private static final String EXTRA_HOST_AND_MAYBE_PORT = "host";
  private static final String EXTRA_NICKNAME = "nickname";

  private SimpleIRCConnectionManager mSimpleIRCConnectionManager;

  private ExecutorService mConnectionExecutor;
  private boolean mIsTearingDown;

  private ListView mConsoleDisplay;
  private IRCConsoleRowAdapter mConsoleRowAdapter;
  private TextView mConsoleInput;

  public static void showForResult(
      Activity context,
      int requestCode,
      String hostAndMaybePort,
      String nickname) {
    Intent intent = new Intent(context, IRCChatActivity.class);
    intent.putExtra(EXTRA_HOST_AND_MAYBE_PORT, hostAndMaybePort);
    intent.putExtra(EXTRA_NICKNAME, nickname);
    context.startActivityForResult(intent, requestCode);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.irc_chat_activity);

    mConsoleDisplay = (ListView) findViewById(R.id.console_display);
    mConsoleRowAdapter = new IRCConsoleRowAdapter(this);
    mConsoleDisplay.setAdapter(mConsoleRowAdapter);

    mConsoleInput = (TextView) findViewById(R.id.console_input);
    mConsoleInput.setOnEditorActionListener(mOnConsoleInputEditorAction);
    findViewById(R.id.console_send).setOnClickListener(mConsoleSendClicked);

    // Will re-enable once we connect...
    mConsoleInput.setEnabled(false);

    mSimpleIRCConnectionManager = new SimpleIRCConnectionManager(
        getIntent().getStringExtra(EXTRA_HOST_AND_MAYBE_PORT),
        getIntent().getStringExtra(EXTRA_NICKNAME));
    mConnectionExecutor = Executors.newCachedThreadPool();
    mConnectionExecutor.execute(new Runnable() {
      @Override
      public void run() {
        mSimpleIRCConnectionManager.runConnectLoop();
      }
    });
  }

  @Override
  protected void onDestroy() {
    mSimpleIRCConnectionManager.shutdown();
    mConnectionExecutor.shutdown();
    mIsTearingDown = true;
    super.onDestroy();
  }

  private void onConnected() {
    mConsoleInput.setEnabled(true);
  }

  private void onIncomingMessage(String message) {
    if (mIsTearingDown) {
      return;
    }
    mConsoleRowAdapter.add(message);
  }

  private void onDisconnectOrConnectFailed(@Nullable IOException exception) {
    if (mIsTearingDown) {
      return;
    }

    mIsTearingDown = true;

    final String error;
    if (exception != null) {
      Toast.makeText(
          this,
          "Error: " + exception.getMessage(),
          Toast.LENGTH_LONG)
          .show();
      error = exception.getMessage();
    } else {
      error = null;
    }
    new IRCChatActivityResult(error).setResult(this);
    finish();
  }

  private final TextView.OnEditorActionListener mOnConsoleInputEditorAction =
      new TextView.OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      switch (actionId) {
        case EditorInfo.IME_ACTION_SEND:
          doSendMessage();
          return true;
        default:
          return false;
      }
    }
  };

  private final View.OnClickListener mConsoleSendClicked = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      doSendMessage();
    }
  };

  private void doSendMessage() {
    final String message = mConsoleInput.getText().toString();
    mConsoleInput.setText("");
    if (!TextUtils.isEmpty(message)) {
      mConnectionExecutor.execute(new Runnable() {
        @Override
        public void run() {
          mSimpleIRCConnectionManager.send(message);
        }
      });
    }
  }

  private class SimpleIRCConnectionManager {
    @Nullable private volatile IRCClientConnection mConnection;
    private volatile boolean mShutdownRequested;

    private final String mHost;
    private final int mPort;
    private final String mNickname;

    public SimpleIRCConnectionManager(String hostAndPort, String nickname) {
      String[] hostAndPortParts = hostAndPort.split(":", 2);
      if (hostAndPortParts.length == 2) {
        mHost = hostAndPortParts[0];
        mPort = Integer.parseInt(hostAndPortParts[1]);
      } else {
        mHost = hostAndPort;
        mPort = DEFAULT_PORT;
      }
      mNickname = nickname;
    }

    public void runConnectLoop() {
      boolean graceful = false;
      try {
        final IRCClientConnection conn = IRCClientConnection.connect(mHost, mPort);
        try {
          mConnection = conn;
          doConnectLoop(conn);
        } finally {
          mConnection = null;
          conn.close();
        }
        graceful = true;
      } catch (IOException e) {
        invokeOnDisconnectOrConnectFailed(e);
      } finally {
        if (graceful) {
          invokeOnDisconnectOrConnectFailed(null /* exception */);
        }
      }
    }

    private void doConnectLoop(IRCClientConnection conn) throws IOException {
      if (mShutdownRequested) {
        return;
      }

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          onConnected();
        }
      });

      conn.send(String.format(Locale.US, "NICK %s", mNickname));
      conn.send(String.format(Locale.US, "USER %s %s blablabla :%s", mNickname, mHost, mNickname));
      while (!mShutdownRequested) {
        final String message = conn.read();
        if (message == null) {
          break;
        }
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            onIncomingMessage(message);
          }
        });
      }
    }

    public void send(String message) {
      IRCClientConnection conn = mConnection;
      if (conn != null) {
        try {
          conn.send(message);
        } catch (IOException e) {
          invokeOnDisconnectOrConnectFailed(e);
        }
      }
    }

    public void shutdown() {
      mShutdownRequested = true;

      // Force a socket closure to cause an immediate effect in Stetho.
      IRCClientConnection conn = mConnection;
      if (conn != null) {
        try {
          conn.close();
        } catch (IOException e) {
          invokeOnDisconnectOrConnectFailed(e);
        }
      }
    }

    private void invokeOnDisconnectOrConnectFailed(@Nullable final IOException e) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          onDisconnectOrConnectFailed(e);
        }
      });
    }
  }

  private static class IRCConsoleRowAdapter extends ArrayAdapter<String> {
    public IRCConsoleRowAdapter(Context context) {
      super(context, R.layout.irc_console_row);
    }
  }

  public static class IRCChatActivityResult {
    private static final String EXTRA_RESULT_CONNECT_ERROR = "error";

    @Nullable public final String connectError;

    public static IRCChatActivityResult fromResult(int resultCode, Intent data) {
      if (resultCode == RESULT_CANCELED) {
        return new IRCChatActivityResult(null /* connectError */);
      } else {
        return new IRCChatActivityResult(data.getStringExtra(EXTRA_RESULT_CONNECT_ERROR));
      }
    }

    public IRCChatActivityResult(@Nullable String connectError) {
      this.connectError = connectError;
    }

    public boolean wasUserDisconnect() {
      return connectError == null;
    }

    private void setResult(Activity activity) {
      if (wasUserDisconnect()) {
        activity.setResult(RESULT_CANCELED);
      } else {
        activity.setResult(
            RESULT_OK,
            new Intent(activity, IRCChatActivity.class)
                .putExtra(EXTRA_RESULT_CONNECT_ERROR, connectError));
      }
    }
  }
}

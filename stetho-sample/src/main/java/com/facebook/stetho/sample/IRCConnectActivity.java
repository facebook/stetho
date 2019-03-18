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
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.stetho.sample.IRCChatActivity.IRCChatActivityResult;

import java.util.Random;

public class IRCConnectActivity extends Activity {
  private static final String DEFAULT_HOST = "irc.freenode.net";

  private static final int REQUEST_CODE_CHAT = 1;

  private TextView mIRCPriorError;
  private EditText mIRCServer;
  private EditText mIRCNickname;

  public static void show(Context context) {
    Intent intent = new Intent(context, IRCConnectActivity.class);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.irc_connect_activity);

    mIRCPriorError = (TextView) findViewById(R.id.irc_prior_error);
    mIRCServer = (EditText) findViewById(R.id.irc_server);
    if (TextUtils.isEmpty(mIRCServer.getText())) {
      mIRCServer.setText(DEFAULT_HOST);
    }
    mIRCNickname = (EditText) findViewById(R.id.irc_nickname);
    if (TextUtils.isEmpty(mIRCNickname.getText())) {
      mIRCNickname.setText("stetho" + (new Random().nextInt(9999) + 1));
    }

    findViewById(R.id.irc_connect).setOnClickListener(mConnectClicked);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_CHAT:
        IRCChatActivityResult parsedResult =
            IRCChatActivityResult.fromResult(resultCode, data);
        if (parsedResult.wasUserDisconnect()) {
          mIRCPriorError.setText("");
          mIRCPriorError.setVisibility(View.GONE);
        } else {
          mIRCPriorError.setText("ERROR: " + parsedResult.connectError);
          mIRCPriorError.setVisibility(View.VISIBLE);
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown requestCode=" + requestCode);
    }
  }

  private final View.OnClickListener mConnectClicked = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      IRCChatActivity.showForResult(
          IRCConnectActivity.this,
          REQUEST_CODE_CHAT,
          mIRCServer.getText().toString(),
          mIRCNickname.getText().toString());
    }
  };
}

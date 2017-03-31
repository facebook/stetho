package com.facebook.stetho.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

/**
 * Created by axolotl on 16/11/17.
 */

public class NpeActivity extends Activity {

    public static void show(Context context) {
        context.startActivity(new Intent(context, NpeActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.npe_activity);
    }
}

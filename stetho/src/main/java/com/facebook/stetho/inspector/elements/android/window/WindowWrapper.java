package com.facebook.stetho.inspector.elements.android.window;

import android.view.View;

public class WindowWrapper {
	private View mView;

	public View getView() {
		return mView;
	}

	public WindowWrapper(View view) {
		this.mView = view;
	}
}

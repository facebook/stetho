package com.facebook.stetho.inspector.elements.android;

import android.view.View;

import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.android.window.WindowFetcher;
import com.facebook.stetho.inspector.elements.android.window.WindowWrapper;

import java.util.List;

import static com.facebook.stetho.inspector.elements.android.ApplicationDescriptor.belongToActivity;


public class RealWindowDescriptor extends AbstractChainedDescriptor<WindowWrapper> {
	static {
		WindowFetcher.hook();
	}

	@Override
	protected void onGetChildren(WindowWrapper element, Accumulator<Object> children) {
		List<View> windows = WindowFetcher.getWindowViews(element.getView().getContext());
		if (windows == null) {
			return;
		}
		ActivityTracker tracker = ActivityTracker.get();
		for (View view : windows) {
			if (belongToActivity(view, tracker.getActivitiesView())) {
				continue;
			}
			children.store(view);
		}
	}
}

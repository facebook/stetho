package com.facebook.stetho.inspector.elements.android.window;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WindowRootViewCompactV16Impl extends WindowRootViewCompat {
	private Context mContext;

	WindowRootViewCompactV16Impl(Context context) {
		this.mContext = context;
	}

	@NonNull
	@Override
	public List<View> getRootViews() {
		WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		if (windowManager == null) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc. windowManager == null !");
		}
		Object wm = getOuter(windowManager);
		return getWindowViews(wm);
	}

	private static Object getOuter(Object innerWM) {
		try {
			Field parentField = innerWM.getClass().getDeclaredField("mWindowManager");
			parentField.setAccessible(true);
			Object outerWM = parentField.get(innerWM);
			parentField.setAccessible(false);
			return outerWM;
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		}
	}

	private static List<View> getWindowViews(final Object windowManager) {
		try {
			Class clz = windowManager.getClass();
			Field field = clz.getDeclaredField("mViews");
			field.setAccessible(true);
			return Collections.unmodifiableList(Arrays.asList((View[]) field.get(windowManager)));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		}
	}
}

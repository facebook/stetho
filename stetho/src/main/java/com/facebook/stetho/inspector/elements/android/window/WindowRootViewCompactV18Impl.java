package com.facebook.stetho.inspector.elements.android.window;

import android.support.annotation.NonNull;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WindowRootViewCompactV18Impl extends WindowRootViewCompat {

	private Field mViewsField;
	private Object mWindowManagerGlobal;

	WindowRootViewCompactV18Impl() {
		try {
			Class wmClz = Class.forName("android.view.WindowManagerGlobal");
			Method getInstanceMethod = wmClz.getDeclaredMethod("getInstance");
			mWindowManagerGlobal = getInstanceMethod.invoke(wmClz);
			mViewsField = wmClz.getDeclaredField("mViews");
			mViewsField.setAccessible(true);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		}

	}

	@NonNull
	@Override
	public List<View> getRootViews() {
		try {
			return Collections.unmodifiableList(Arrays.asList((View[]) mViewsField.get(mWindowManagerGlobal)));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("unfortunately, you cannot view the view tree of the dialog etc.", e);
		}
	}
}

package com.facebook.stetho.inspector.elements.android.window;

import android.support.annotation.NonNull;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class WindowRootViewCompactV19Impl extends WindowRootViewCompat {

	private List<View> mRootViews;

	WindowRootViewCompactV19Impl() {
		try {
			Class wmClz = Class.forName("android.view.WindowManagerGlobal");
			Method getInstanceMethod = wmClz.getDeclaredMethod("getInstance");
			Object managerGlobal = getInstanceMethod.invoke(wmClz);
			Field mViewsFiled = wmClz.getDeclaredField("mViews");
			mViewsFiled.setAccessible(true);
			mRootViews = Collections.unmodifiableList((List<View>) mViewsFiled.get(managerGlobal));
			mViewsFiled.setAccessible(false);
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
		return mRootViews;
	}
}

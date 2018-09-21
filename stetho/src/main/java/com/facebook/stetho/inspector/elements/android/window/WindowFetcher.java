package com.facebook.stetho.inspector.elements.android.window;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

import com.facebook.stetho.common.Util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/*
 * 4.0.3_r1    WindowManagerImpl   private View[] mViews;
 * 4.0.4       WindowManagerImpl   private View[] mViews;
 *
 * 4.1.1       WindowManagerImpl   private View[] mViews;
 * 4.1.2       WindowManagerImpl   private View[] mViews;
 *
 * 4.2_r1      WindowManagerGlobal  private View[] mViews
 * 4.2.2 r1    WindowManagerGlobal  private View[] mViews
 *
 * 4.3_r2.1     WindowManagerGlobal  private View[] mViews;
 *
 * 4.4_r1       WindowManagerGlobal   private final ArrayList<View> mViews
 * 4.4.2_r1     WindowManagerGlobal   private final ArrayList<View> mViews
 *
 * 5.0.0_r2     WindowManagerGlobal   private final ArrayList<View> mViews
 *
 * 6.0.0_r1     WindowManagerGlobal   private final ArrayList<View> mViews
 *
 * 7.0.0_r1     WindowManagerGlobal   private final ArrayList<View> mViews
 *
 * 8.0.0_r4    WindowManagerGlobal   private final ArrayList<View> mViews
 */

public class WindowFetcher {
	public static List<View> getWindowViews(Context context) {
		Util.throwIfNull(context);
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
			WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Object wm = getOuter(windowManager);
			if (wm == null) {
				return null;
			}
			return getWindowViews(wm);
		} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1
			|| Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
			WindowManagerGlobal managerGlobal = WindowManagerGlobal.getInstance();
			return getWindowViews(managerGlobal);

		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			return sWindow;
		}
		return null;
	}

	private static List<View> sWindow;

	public static void hook() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			WindowManagerGlobal managerGlobal = WindowManagerGlobal.getInstance();
			try {
				Field mViewsFiled = WindowManagerGlobal.class.getDeclaredField("mViews");
				mViewsFiled.setAccessible(true);
				sWindow = Collections.unmodifiableList((List<View>) mViewsFiled.get(managerGlobal));
				mViewsFiled.setAccessible(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static Object getOuter(Object innerWM) {
		try {
			Field parentFiled = innerWM.getClass().getDeclaredField("mWindowManager");
			parentFiled.setAccessible(true);
			Object outerWM = parentFiled.get(innerWM);
			parentFiled.setAccessible(false);
			return outerWM;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static List<View> getWindowViews(final Object obj) {
		try {
			Class clz = obj.getClass();
			final Field field = clz.getDeclaredField("mViews");
			field.setAccessible(true);
			return Collections.unmodifiableList(Arrays.asList((View[]) field.get(obj)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}

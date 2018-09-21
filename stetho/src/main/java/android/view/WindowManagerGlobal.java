package android.view;

import android.support.annotation.Keep;

/**
 * Created by wanjian on 2018/9/11.
 * <p>
 * <p>
 * A simple way to get WindowManagerGlobal
 * <p>
 * WindowManagerGlobal is hide,so we can not use it,but we can create a same name class.
 * <p>
 * <p>
 * declare -keep class android.view.WindowManagerGlobal{*;} in your proguard-rules.pro file
 */
@Keep
public class WindowManagerGlobal {
    public static WindowManagerGlobal getInstance() {
        return null;
    }
}

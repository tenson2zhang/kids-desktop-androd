package com.kidsdesktop.launcher;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ImePackages {
    private ImePackages() {
    }

    public static Set<String> current(Context context) {
        Set<String> packages = new HashSet<String>();
        InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager == null) {
            return packages;
        }
        List<InputMethodInfo> methods = manager.getEnabledInputMethodList();
        if (methods == null) {
            return packages;
        }
        for (InputMethodInfo info : methods) {
            if (info != null && info.getPackageName() != null) {
                packages.add(info.getPackageName());
            }
        }
        return packages;
    }
}

package com.kidsdesktop.launcher;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;

/** 家长离开到系统设置时的短时放行。回到本应用窗口后立即收回。 */
public final class EscapeWindow {
    private static long ignoreClearUntil;

    private EscapeWindow() {
    }

    public static void arm(Context context, long durationMs) {
        Prefs.allowEscape(context, durationMs);
        ignoreClearUntil = SystemClock.elapsedRealtime() + 1200L;
        LauncherRoles.invalidate();
    }

    public static void clearWhenFocused(Activity activity) {
        if (!Prefs.isEscapeActive(activity)) {
            return;
        }
        activity.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) {
                    return;
                }
                if (SystemClock.elapsedRealtime() < ignoreClearUntil) {
                    return;
                }
                if (!activity.hasWindowFocus()) {
                    return;
                }
                Prefs.clearEscape(activity);
                LauncherRoles.invalidate();
            }
        });
    }
}

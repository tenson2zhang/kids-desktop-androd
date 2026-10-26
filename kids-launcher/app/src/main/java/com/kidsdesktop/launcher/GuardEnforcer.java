package com.kidsdesktop.launcher;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import com.kidsdesktop.core.GuardPolicy;
import java.util.Set;

public final class GuardEnforcer {
    private static final String TAG = "KidsDesktop";
    private static long lastBounce;
    private static long imeAt;
    private static Set<String> imeCache;

    private GuardEnforcer() {
    }

    public static void onForeground(Context context, String packageName, String className) {
        if (!Prefs.isSetupComplete(context)) {
            return;
        }
        if (Prefs.isEscapeActive(context)) {
            return;
        }
        if (!LauncherRoles.isDefaultHome(context)) {
            return;
        }
        GuardPolicy.Decision decision = GuardPolicy.decide(
                packageName,
                className,
                context.getPackageName(),
                Prefs.whitelist(context),
                imes(context),
                false);
        if (decision.allow) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastBounce < 450L) {
            return;
        }
        lastBounce = now;
        Log.i(TAG, "block " + packageName + " / " + className + " (" + decision.reason + ")");
        bringHome(context);
        if (GuardPolicy.canStopPackage(packageName)) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                manager.killBackgroundProcesses(packageName);
            }
        }
        if (context instanceof AccessibilityService) {
            ((AccessibilityService) context).performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        }
    }

    private static Set<String> imes(Context context) {
        long now = SystemClock.elapsedRealtime();
        if (imeCache == null || now - imeAt > 10000L) {
            imeCache = ImePackages.current(context);
            imeAt = now;
        }
        return imeCache;
    }

    private static void bringHome(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}

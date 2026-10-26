package com.kidsdesktop.launcher;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

public final class LauncherRoles {
    private static long cachedAt;
    private static boolean cached;

    private LauncherRoles() {
    }

    public static void invalidate() {
        cachedAt = 0L;
    }

    public static boolean isDefaultHome(Context context) {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt != 0L && now - cachedAt < 1500L) {
            return cached;
        }
        cached = query(context);
        cachedAt = now;
        return cached;
    }

    private static boolean query(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null || info.activityInfo.packageName == null) {
            return false;
        }
        String pkg = info.activityInfo.packageName;
        if ("android".equals(pkg) || pkg.contains("resolver")) {
            return false;
        }
        return context.getPackageName().equals(pkg);
    }

    public static void requestAsDefault(Activity activity) {
        invalidate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = activity.getSystemService(RoleManager.class);
            if (roles != null
                    && roles.isRoleAvailable(RoleManager.ROLE_HOME)
                    && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                activity.startActivity(roles.createRequestRoleIntent(RoleManager.ROLE_HOME));
                return;
            }
        }
        openHomeSettings(activity);
    }

    public static void openHomeSettings(Context context) {
        invalidate();
        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(home);
        }
    }
}

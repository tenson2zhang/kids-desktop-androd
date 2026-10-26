package com.kidsdesktop.launcher;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import java.util.ArrayList;

/** 设备所有者模式下的锁定任务。没有设备所有者时什么都不做。 */
public final class LockTaskController {
    private LockTaskController() {
    }

    public static ComponentName admin(Context context) {
        return new ComponentName(context, KidsDeviceAdminReceiver.class);
    }

    public static boolean isDeviceOwner(Context context) {
        DevicePolicyManager manager = manager(context);
        return manager != null && manager.isDeviceOwnerApp(context.getPackageName());
    }

    public static void apply(Context context) {
        if (!isDeviceOwner(context)) {
            return;
        }
        DevicePolicyManager manager = manager(context);
        if (manager == null) {
            return;
        }
        ComponentName admin = admin(context);
        ArrayList<String> packages = new ArrayList<String>();
        packages.add(context.getPackageName());
        packages.addAll(Prefs.whitelist(context));
        packages.add("com.android.incallui");
        packages.add("com.android.server.telecom");
        packages.add("com.android.phone");
        try {
            manager.setLockTaskPackages(admin, packages.toArray(new String[packages.size()]));
        } catch (SecurityException ex) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                manager.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
            } catch (SecurityException ignored) {
                // 旧的设备所有者策略可能不接受这个调用。
            }
        }
        try {
            manager.setStatusBarDisabled(admin, true);
        } catch (SecurityException ignored) {
            // 状态栏禁用失败时，无障碍守护仍然会拦通知栏。
        }
        try {
            manager.setKeyguardDisabled(admin, true);
        } catch (SecurityException ignored) {
            // 锁屏关不掉时，用户解锁后仍会进入默认桌面。
        }
    }

    public static void start(Activity activity) {
        if (!isDeviceOwner(activity) || Prefs.isEscapeActive(activity)) {
            return;
        }
        try {
            activity.startLockTask();
        } catch (IllegalArgumentException ignored) {
            // 当前任务还不能进入锁定时，下次回到桌面再试。
        }
    }

    public static void suspendForEscape(Activity activity) {
        if (!isDeviceOwner(activity)) {
            return;
        }
        try {
            activity.stopLockTask();
        } catch (IllegalArgumentException ignored) {
            // 当前没有处于锁定任务。
        }
        DevicePolicyManager manager = manager(activity);
        if (manager == null) {
            return;
        }
        ComponentName admin = admin(activity);
        try {
            manager.setStatusBarDisabled(admin, false);
        } catch (SecurityException ignored) {
            // 恢复状态栏失败不阻止打开系统设置。
        }
        try {
            manager.setLockTaskPackages(admin, new String[] {
                    activity.getPackageName(),
                    "com.android.settings",
                    "com.android.systemui",
                    "com.android.permissioncontroller",
                    "com.google.android.permissioncontroller"
            });
        } catch (SecurityException ignored) {
            // 放行系统设置失败时，家长仍可从已停止的锁定任务里操作。
        }
    }

    private static DevicePolicyManager manager(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }
}

package com.kidsdesktop.launcher;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import java.lang.reflect.Method;

/**
 * 发起关机。普通应用通常没有关机权限，能成功的是系统允许默认桌面调用关机接口的设备。
 * 不尝试 root。
 */
public final class ShutdownHelper {
    private ShutdownHelper() {
    }

    public static boolean requestShutdown(Context context) {
        if (tryIntent(context, "com.android.internal.intent.action.REQUEST_SHUTDOWN")) {
            return true;
        }
        if (tryIntent(context, "android.intent.action.ACTION_REQUEST_SHUTDOWN")) {
            return true;
        }
        return tryPowerManager(context);
    }

    private static boolean tryIntent(Context context, String action) {
        try {
            Intent intent = new Intent(action);
            intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
            intent.putExtra("android.intent.extra.USER_REQUESTED_SHUTDOWN", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean tryPowerManager(Context context) {
        try {
            PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (manager == null) {
                return false;
            }
            try {
                Method method = manager.getClass().getMethod("shutdown", boolean.class, String.class, boolean.class);
                method.invoke(manager, false, "userrequested", false);
                return true;
            } catch (NoSuchMethodException ignored) {
                Method method = manager.getClass().getMethod("shutdown", boolean.class, String.class);
                method.invoke(manager, false, "userrequested");
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }
}

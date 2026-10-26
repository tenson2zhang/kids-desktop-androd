package com.kidsdesktop.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Prefs.isSetupComplete(context)) {
            return;
        }
        WatchdogService.start(context);
        LockTaskController.apply(context);
        if (!LauncherRoles.isDefaultHome(context)) {
            return;
        }
        Intent home = new Intent(context, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(home);
        } catch (RuntimeException ignored) {
            // 系统若拒绝开机时拉起界面，默认桌面仍会由系统自己打开。
        }
    }
}

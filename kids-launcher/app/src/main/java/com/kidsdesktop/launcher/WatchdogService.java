package com.kidsdesktop.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.List;

/** 使用情况权限打开后，每秒核对一次前台应用，补无障碍漏掉的切换。 */
public class WatchdogService extends Service {
    private static final int NOTIFICATION_ID = 7;
    private static final String CHANNEL_ID = "kids_guard";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            checkForeground();
            handler.postDelayed(this, 1000L);
        }
    };

    public static void start(Context context) {
        if (!Prefs.isSetupComplete(context)) {
            return;
        }
        Intent intent = new Intent(context, WatchdogService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (RuntimeException ignored) {
            // 系统若暂时不允许拉起前台服务，下次回到桌面再试。
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (RuntimeException ex) {
            stopSelf();
            return;
        }
        handler.post(tick);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkForeground() {
        if (!Access.hasUsageAccess(this)) {
            return;
        }
        UsageStatsManager stats = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        if (stats == null) {
            return;
        }
        long now = System.currentTimeMillis();
        List<UsageStats> recent = stats.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000L, now);
        if (recent == null || recent.isEmpty()) {
            return;
        }
        UsageStats latest = null;
        for (UsageStats item : recent) {
            if (latest == null || item.getLastTimeUsed() > latest.getLastTimeUsed()) {
                latest = item;
            }
        }
        if (latest == null || now - latest.getLastTimeUsed() > 8000L) {
            return;
        }
        GuardEnforcer.onForeground(this, latest.getPackageName(), null);
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "儿童桌面保护",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("儿童桌面正在保护")
                .setContentText("只有白名单里的应用可以留在前台")
                .setOngoing(true)
                .build();
    }
}

package com.kidsdesktop.launcher;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class KidsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (Prefs.isSetupComplete(this)) {
            WatchdogService.start(this);
            LockTaskController.apply(this);
        }
    }
}

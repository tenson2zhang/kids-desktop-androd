package com.kidsdesktop.launcher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InstalledApps {
    private InstalledApps() {
    }

    public static List<AppEntry> launcherApps(PackageManager pm, String ownPackage) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
        Map<String, AppEntry> byPackage = new LinkedHashMap<String, AppEntry>();
        if (resolved != null) {
            for (ResolveInfo info : resolved) {
                if (info.activityInfo == null) {
                    continue;
                }
                String pkg = info.activityInfo.packageName;
                if (pkg == null || pkg.equals(ownPackage) || byPackage.containsKey(pkg)) {
                    continue;
                }
                CharSequence label = info.loadLabel(pm);
                Drawable icon = null;
                try {
                    icon = info.loadIcon(pm);
                } catch (RuntimeException ignored) {
                    icon = null;
                }
                byPackage.put(pkg, new AppEntry(pkg, label == null ? pkg : label.toString(), icon));
            }
        }
        List<AppEntry> apps = new ArrayList<AppEntry>(byPackage.values());
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                return collator.compare(left.label, right.label);
            }
        });
        return apps;
    }
}

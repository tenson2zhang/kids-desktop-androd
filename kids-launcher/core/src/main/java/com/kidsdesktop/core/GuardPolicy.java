package com.kidsdesktop.core;

import java.util.Locale;

/**
 * 决定前台窗口能不能留在屏幕上。纯逻辑，不依赖安卓框架，方便测试。
 * 家长放行期间全部允许。其余时候只有本桌面、白名单、输入法和来电界面可以留在前台。
 * 最近任务、通知栏这类系统窗口单独拦下。
 */
public final class GuardPolicy {

    private GuardPolicy() {
    }

    public static final class Decision {
        public final boolean allow;
        public final String reason;

        public Decision(boolean allow, String reason) {
            this.allow = allow;
            this.reason = reason;
        }
    }

    public static Decision decide(
            String eventPackage,
            String className,
            String ownPackage,
            java.util.Set<String> whitelist,
            java.util.Set<String> imePackages,
            boolean parentEscapeActive) {
        if (parentEscapeActive) {
            return new Decision(true, "parent-escape");
        }
        if (eventPackage == null || eventPackage.length() == 0) {
            return new Decision(true, "empty-package");
        }
        if (ownPackage != null && ownPackage.equals(eventPackage)) {
            return new Decision(true, "self");
        }
        if (isEmergencyOrInCall(eventPackage, className)) {
            return new Decision(true, "call");
        }
        if (imePackages != null && imePackages.contains(eventPackage)) {
            return new Decision(true, "ime");
        }
        if (whitelist != null && whitelist.contains(eventPackage)) {
            return new Decision(true, "whitelist");
        }
        if (isSystemUi(eventPackage)) {
            if (isBlockedSystemWindow(className)) {
                return new Decision(false, "system-window");
            }
            return new Decision(true, "system-chrome");
        }
        if (isResolver(eventPackage, className)) {
            return new Decision(false, "resolver");
        }
        return new Decision(false, "not-whitelisted");
    }

    /** 不允许结束系统界面和通话进程，避免把状态栏或来电打崩。 */
    public static boolean canStopPackage(String packageName) {
        if (packageName == null || packageName.length() == 0) {
            return false;
        }
        if ("android".equals(packageName)) {
            return false;
        }
        String lower = packageName.toLowerCase(Locale.US);
        if (lower.contains("systemui") || lower.contains("incallui")) {
            return false;
        }
        return !"com.android.phone".equals(packageName)
                && !"com.android.server.telecom".equals(packageName);
    }

    /** 这些应用容易变成离开儿童桌面的出口，白名单界面上会标出来。家长仍可以勾选。 */
    public static boolean isRisky(String packageName, String ownPackage) {
        if (packageName == null || packageName.length() == 0) {
            return false;
        }
        if (packageName.equals(ownPackage)) {
            return false;
        }
        String value = packageName.toLowerCase(Locale.US);
        return containsAny(value,
                "settings",
                "launcher",
                "trebuchet",
                "browser",
                "chrome",
                "firefox",
                "sbrowser",
                "opera",
                "vending",
                "packageinstaller",
                "documentsui",
                "market",
                "appstore",
                "playstore");
    }

    static boolean isSystemUi(String packageName) {
        String value = packageName.toLowerCase(Locale.US);
        return "com.android.systemui".equals(value) || value.endsWith(".systemui");
    }

    static boolean isBlockedSystemWindow(String className) {
        if (className == null || className.length() == 0) {
            return false;
        }
        String value = className.toLowerCase(Locale.US);
        return containsAny(value,
                "recents",
                "overview",
                "quickstep",
                "notificationpanel",
                "notificationshade",
                "qspanel",
                "quicksettings",
                "taskview",
                "recentsview",
                "notificationstack",
                "shadeview");
    }

    static boolean isEmergencyOrInCall(String packageName, String className) {
        String pkg = packageName.toLowerCase(Locale.US);
        String cls = className == null ? "" : className.toLowerCase(Locale.US);
        if (cls.contains("emergency") || cls.contains("incall")) {
            return true;
        }
        return pkg.contains("incallui") || "com.android.server.telecom".equals(pkg);
    }

    static boolean isResolver(String packageName, String className) {
        String pkg = packageName.toLowerCase(Locale.US);
        String cls = className == null ? "" : className.toLowerCase(Locale.US);
        if (pkg.contains("intentresolver")) {
            return true;
        }
        return "android".equals(pkg) && cls.contains("resolver");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

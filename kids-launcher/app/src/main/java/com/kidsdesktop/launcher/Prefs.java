package com.kidsdesktop.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import com.kidsdesktop.core.AttemptLimiter;
import com.kidsdesktop.core.PasswordHasher;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Prefs {
    public static final long PARENT_UNLOCK_MS = 5 * 60 * 1000L;
    public static final long ESCAPE_MS = 90 * 1000L;

    private static final String NAME = "kids_desktop";
    private static final String SALT = "password_salt";
    private static final String HASH = "password_hash";
    private static final String SETUP = "setup_complete";
    private static final String WHITELIST = "whitelist";
    private static final String ESCAPE_BOOT = "escape_boot";
    private static final String ESCAPE_UNTIL = "escape_until";
    private static final String UNLOCK_BOOT = "unlock_boot";
    private static final String UNLOCK_UNTIL = "unlock_until";
    private static final String ATTEMPT_BOOT = "attempt_boot";
    private static final String FAILS = "fail_count";
    private static final String LOCK_UNTIL = "lock_until";

    private Prefs() {
    }

    private static SharedPreferences raw(Context context) {
        return context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean hasPassword(Context context) {
        SharedPreferences prefs = raw(context);
        String salt = prefs.getString(SALT, "");
        String hash = prefs.getString(HASH, "");
        return salt != null && salt.length() > 0 && hash != null && hash.length() > 0;
    }

    public static void setPassword(Context context, String password) {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hashHex(salt, password);
        raw(context).edit().putString(SALT, salt).putString(HASH, hash).commit();
    }

    public static boolean verifyPassword(Context context, String password) {
        SharedPreferences prefs = raw(context);
        return PasswordHasher.verify(prefs.getString(SALT, null), prefs.getString(HASH, null), password);
    }

    public static boolean isSetupComplete(Context context) {
        return raw(context).getBoolean(SETUP, false);
    }

    public static void setSetupComplete(Context context, boolean complete) {
        raw(context).edit().putBoolean(SETUP, complete).commit();
    }

    public static Set<String> whitelist(Context context) {
        Set<String> stored = raw(context).getStringSet(WHITELIST, Collections.<String>emptySet());
        if (stored == null) {
            return new HashSet<String>();
        }
        return new HashSet<String>(stored);
    }

    public static void setWhitelist(Context context, Set<String> packages) {
        raw(context).edit().putStringSet(WHITELIST, new HashSet<String>(packages)).commit();
    }

    public static void allowEscape(Context context, long durationMs) {
        raw(context).edit()
                .putString(ESCAPE_BOOT, BootId.current())
                .putLong(ESCAPE_UNTIL, SystemClock.elapsedRealtime() + durationMs)
                .commit();
    }

    public static void clearEscape(Context context) {
        raw(context).edit().remove(ESCAPE_BOOT).putLong(ESCAPE_UNTIL, 0L).commit();
    }

    public static boolean isEscapeActive(Context context) {
        SharedPreferences prefs = raw(context);
        if (!BootId.current().equals(prefs.getString(ESCAPE_BOOT, ""))) {
            return false;
        }
        return SystemClock.elapsedRealtime() < prefs.getLong(ESCAPE_UNTIL, 0L);
    }

    public static void unlock(Context context, long durationMs) {
        raw(context).edit()
                .putString(UNLOCK_BOOT, BootId.current())
                .putLong(UNLOCK_UNTIL, SystemClock.elapsedRealtime() + durationMs)
                .commit();
    }

    public static void lock(Context context) {
        raw(context).edit()
                .remove(UNLOCK_BOOT)
                .putLong(UNLOCK_UNTIL, 0L)
                .remove(ESCAPE_BOOT)
                .putLong(ESCAPE_UNTIL, 0L)
                .commit();
    }

    public static boolean isUnlocked(Context context) {
        SharedPreferences prefs = raw(context);
        if (!BootId.current().equals(prefs.getString(UNLOCK_BOOT, ""))) {
            return false;
        }
        return SystemClock.elapsedRealtime() < prefs.getLong(UNLOCK_UNTIL, 0L);
    }

    public static AttemptLimiter.State attempts(Context context) {
        SharedPreferences prefs = raw(context);
        if (!BootId.current().equals(prefs.getString(ATTEMPT_BOOT, ""))) {
            return new AttemptLimiter.State(0, 0L);
        }
        return new AttemptLimiter.State(prefs.getInt(FAILS, 0), prefs.getLong(LOCK_UNTIL, 0L));
    }

    public static void saveAttempts(Context context, AttemptLimiter.State state) {
        raw(context).edit()
                .putString(ATTEMPT_BOOT, BootId.current())
                .putInt(FAILS, state.failures)
                .putLong(LOCK_UNTIL, state.lockUntilMillis)
                .commit();
    }
}

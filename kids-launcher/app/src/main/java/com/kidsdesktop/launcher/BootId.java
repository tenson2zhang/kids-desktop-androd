package com.kidsdesktop.launcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** 同一次开机内的标识。用来让家长放行和输错锁定在重启后失效。 */
public final class BootId {
    private static String cached;

    private BootId() {
    }

    public static synchronized String current() {
        if (cached != null) {
            return cached;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/sys/kernel/random/boot_id"));
            String line = reader.readLine();
            cached = line == null ? "" : line.trim();
        } catch (IOException ex) {
            cached = "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // 读不到开机标识时，放行窗口按空标识处理。
                }
            }
        }
        return cached;
    }
}

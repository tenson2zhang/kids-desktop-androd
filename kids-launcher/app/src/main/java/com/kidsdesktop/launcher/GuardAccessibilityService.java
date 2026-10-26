package com.kidsdesktop.launcher;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

public class GuardAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = event.getPackageName();
            CharSequence cls = event.getClassName();
            GuardEnforcer.onForeground(
                    this,
                    pkg == null ? null : pkg.toString(),
                    cls == null ? null : cls.toString());
            return;
        }
        if (type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return;
        }
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) {
            return;
        }
        for (AccessibilityWindowInfo window : windows) {
            if (window == null) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                continue;
            }
            CharSequence pkg = root.getPackageName();
            CharSequence cls = root.getClassName();
            GuardEnforcer.onForeground(
                    this,
                    pkg == null ? null : pkg.toString(),
                    cls == null ? null : cls.toString());
            root.recycle();
        }
    }

    @Override
    public void onInterrupt() {
    }
}

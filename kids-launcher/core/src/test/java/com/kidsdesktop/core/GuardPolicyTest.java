package com.kidsdesktop.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class GuardPolicyTest {

    private static final String SELF = "com.kidsdesktop.launcher";

    @Test
    public void parentEscapeAllowsAnything() {
        GuardPolicy.Decision decision = decide("com.android.settings", "Settings", true);
        assertTrue(decision.allow);
        assertEquals("parent-escape", decision.reason);
    }

    @Test
    public void emptyPackageIsIgnored() {
        GuardPolicy.Decision decision = decide(null, null, false);
        assertTrue(decision.allow);
        assertEquals("empty-package", decision.reason);
    }

    @Test
    public void ownLauncherIsAllowed() {
        GuardPolicy.Decision decision = decide(SELF, "HomeActivity", false);
        assertTrue(decision.allow);
        assertEquals("self", decision.reason);
    }

    @Test
    public void whitelistAllowsApp() {
        GuardPolicy.Decision decision = GuardPolicy.decide(
                "com.example.draw",
                "DrawActivity",
                SELF,
                setOf("com.example.draw"),
                Collections.<String>emptySet(),
                false);
        assertTrue(decision.allow);
        assertEquals("whitelist", decision.reason);
    }

    @Test
    public void otherAppIsBlocked() {
        GuardPolicy.Decision decision = decide("com.example.game", "GameActivity", false);
        assertFalse(decision.allow);
        assertEquals("not-whitelisted", decision.reason);
    }

    @Test
    public void enabledImeIsAllowed() {
        GuardPolicy.Decision decision = GuardPolicy.decide(
                "com.google.android.inputmethod.pinyin",
                "InputMethod",
                SELF,
                Collections.<String>emptySet(),
                setOf("com.google.android.inputmethod.pinyin"),
                false);
        assertTrue(decision.allow);
        assertEquals("ime", decision.reason);
    }

    @Test
    public void recentsWindowIsBlocked() {
        GuardPolicy.Decision decision = decide(
                "com.android.systemui",
                "com.android.systemui.recents.RecentsActivity",
                false);
        assertFalse(decision.allow);
        assertEquals("system-window", decision.reason);
    }

    @Test
    public void quickstepOverviewIsBlocked() {
        assertFalse(decide("com.android.systemui", "com.android.quickstep.views.RecentsView", false).allow);
        assertFalse(decide("com.android.systemui", "com.android.systemui.statusbar.phone.NotificationPanelView", false).allow);
        assertFalse(decide("com.vendor.systemui", "com.vendor.Overview", false).allow);
    }

    @Test
    public void volumeAndUnknownSystemChromeStay() {
        assertTrue(decide("com.android.systemui", "com.android.systemui.volume.VolumeDialogImpl", false).allow);
        assertEquals("system-chrome", decide("com.android.systemui", null, false).reason);
    }

    @Test
    public void callsStayAvailable() {
        assertEquals("call", decide("com.android.incallui", "InCallActivity", false).reason);
        assertEquals("call", decide("com.samsung.android.incallui", "Main", false).reason);
        assertEquals("call", decide("com.android.server.telecom", "Telecom", false).reason);
        assertEquals("call", decide("com.android.phone", "com.android.phone.EmergencyDialer", false).reason);
    }

    @Test
    public void chooserIsBlocked() {
        assertEquals("resolver", decide("android", "com.android.internal.app.ResolverActivity", false).reason);
        assertEquals("resolver", decide("com.android.intentresolver", "Chooser", false).reason);
    }

    @Test
    public void settingsBlockedUnlessParentWhitelistsIt() {
        assertFalse(decide("com.android.settings", "Settings", false).allow);
        GuardPolicy.Decision allowed = GuardPolicy.decide(
                "com.android.settings",
                "Settings",
                SELF,
                setOf("com.android.settings"),
                Collections.<String>emptySet(),
                false);
        assertTrue(allowed.allow);
    }

    @Test
    public void doesNotKillSystemUiOrCalls() {
        assertFalse(GuardPolicy.canStopPackage("com.android.systemui"));
        assertFalse(GuardPolicy.canStopPackage("com.android.incallui"));
        assertFalse(GuardPolicy.canStopPackage("android"));
        assertFalse(GuardPolicy.canStopPackage(null));
        assertTrue(GuardPolicy.canStopPackage("com.example.game"));
    }

    @Test
    public void riskyPackagesAreHintsNotOurOwnApp() {
        assertFalse(GuardPolicy.isRisky(SELF, SELF));
        assertTrue(GuardPolicy.isRisky("com.android.chrome", SELF));
        assertTrue(GuardPolicy.isRisky("com.android.settings", SELF));
        assertTrue(GuardPolicy.isRisky("com.android.vending", SELF));
        assertFalse(GuardPolicy.isRisky("com.example.draw", SELF));
        assertFalse(GuardPolicy.isRisky(null, SELF));
    }

    private static GuardPolicy.Decision decide(String pkg, String className, boolean escape) {
        return GuardPolicy.decide(
                pkg,
                className,
                SELF,
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                escape);
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }
}

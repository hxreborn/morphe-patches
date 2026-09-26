/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String SCHEDULED_DELETION = "Scheduled Trash and Spam deletion";
    static final String AMOLED_DARK_THEME = "AMOLED dark theme";
    static final String ACCENT_COLOR = "Custom accent color";
    static final String HIDE_UPGRADE_PROMOTIONS = "Hide upgrade promotions";

    private AppliedPatches() {}

    public static boolean accentColor() {
        return AccentColor.isPatched();
    }

    public static boolean amoledDarkTheme() {
        return AmoledTheme.isPatched();
    }

    public static boolean hideUpgradePromotions() {
        return UpsellingVisibility.isPatched();
    }

    public static boolean hidePromotionalMessages() {
        return false;
    }

    public static boolean removeSentFromSignature() {
        return false;
    }

    public static boolean removeFreeAccountsLimit() {
        return false;
    }

    public static boolean scheduledDeletion() {
        return false;
    }

    public static boolean unlockCustomTimePicker() {
        return false;
    }

    public static boolean removeServerChangeDelay() {
        return false;
    }

    public static boolean unlockSplitTunneling() {
        return false;
    }

    public static boolean unlockLanConnections() {
        return false;
    }

    public static boolean unlockCustomDns() {
        return false;
    }

    public static boolean unlockNetShield() {
        return false;
    }

    public static boolean unlockConnectionPreferences() {
        return false;
    }

    public static boolean unlockProfiles() {
        return false;
    }

    public static boolean showFreeServerLocations() {
        return false;
    }

    static List<String> names() {
        final List<String> names = new ArrayList<>();
        if (accentColor()) names.add(ACCENT_COLOR);
        if (amoledDarkTheme()) names.add(AMOLED_DARK_THEME);
        if (hideUpgradePromotions()) names.add(HIDE_UPGRADE_PROMOTIONS);
        if (hidePromotionalMessages()) names.add("Hide promotional messages");
        if (removeSentFromSignature()) names.add("Remove 'Sent from' signature");
        if (removeFreeAccountsLimit()) names.add("Remove free accounts limit");
        if (scheduledDeletion()) names.add(SCHEDULED_DELETION);
        if (unlockCustomTimePicker()) names.add("Unlock custom time picker");
        if (removeServerChangeDelay()) names.add("Remove server change delay");
        if (unlockSplitTunneling()) names.add("Unlock split tunneling");
        if (unlockLanConnections()) names.add("Unlock LAN connections");
        if (unlockCustomDns()) names.add("Unlock custom DNS");
        if (unlockNetShield()) names.add("Unlock NetShield");
        if (unlockConnectionPreferences()) names.add("Unlock connection preferences");
        if (unlockProfiles()) names.add("Unlock profiles");
        if (showFreeServerLocations()) names.add("Show free server locations");
        return names;
    }
}

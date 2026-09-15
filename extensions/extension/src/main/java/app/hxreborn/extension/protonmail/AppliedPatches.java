/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String SCHEDULED_DELETION = "Scheduled Trash and Spam deletion";
    static final String AMOLED_DARK_THEME = "AMOLED dark theme";
    static final String HIDE_UPGRADE_PROMOTIONS = "Hide upgrade promotions";

    private AppliedPatches() {}

    public static boolean amoledDarkTheme() {
        return AmoledTheme.isPatched();
    }

    public static boolean hideUpgradePromotions() {
        return UpsellingVisibility.isPatched();
    }

    public static boolean removeSentFromSignature() {
        return false; // stub
    }

    public static boolean removeFreeAccountsLimit() {
        return false; // stub
    }

    public static boolean scheduledDeletion() {
        return false; // stub
    }

    public static boolean unlockCustomTimePicker() {
        return false; // stub
    }

    static List<String> names() {
        final List<String> names = new ArrayList<>();
        if (amoledDarkTheme()) names.add(AMOLED_DARK_THEME);
        if (hideUpgradePromotions()) names.add(HIDE_UPGRADE_PROMOTIONS);
        if (removeSentFromSignature()) names.add("Remove 'Sent from' signature");
        if (removeFreeAccountsLimit()) names.add("Remove free accounts limit");
        if (scheduledDeletion()) names.add(SCHEDULED_DELETION);
        if (unlockCustomTimePicker()) names.add("Unlock custom time picker");
        return names;
    }
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

@SuppressWarnings("unused")
public final class UpsellingVisibility {

    private static final String KEY = "hide_upgrade_promotions";

    private UpsellingVisibility() {}

    public static boolean isPatched() {
        return false; // stub
    }

    public static boolean isHidden() {
        return PatchSettings.isFeatureEnabled(isPatched(), KEY);
    }

    static void setHidden(boolean hidden) {
        PatchSettings.setEnabled(KEY, hidden);
    }
}

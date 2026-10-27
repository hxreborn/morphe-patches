/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.proton;

@SuppressWarnings("unused")
public final class UpsellingVisibility {

    private static final String KEY = "hide_upgrade_promotions";

    private UpsellingVisibility() {

    }

    public static boolean isPatched() {
        return false;
    }

    public static boolean isHidden() {
        return PatchSettings.isFeatureEnabled(isPatched(), KEY);
    }

    public static boolean resolveUpgradeAvailable(boolean available) {
        return available && !isHidden();
    }

    static void setHidden(boolean hidden) {
        PatchSettings.setEnabled(KEY, hidden);
    }

}

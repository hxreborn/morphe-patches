/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

public final class Members {

    private Members() {}

    public static String userInfoVpnUser() {
        return "getVpnUser";
    }

    public static String vpnUserIsFreeUser() {
        return "isFreeUser";
    }

    public static String serverGroupTier() {
        return "getTier";
    }

    public static String profileAvailability() {
        return "getAvailability";
    }

    public static String serverGroupBannerClass() {
        return "com.protonvpn.android.redesign.countries.ui.ServerGroupUiItem$Banner";
    }
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.protonvpn;

public final class Members {

    private Members() {

    }

    public static String userInfoVpnUser() {
        return "vpnUser";
    }

    public static String vpnUserIsFreeUser() {
        return "isFreeUser";
    }

    public static String serverGroupTier() {
        return "getTier";
    }

    public static String profileAvailability() {
        return "availability";
    }

    public static String serverIsFree() {
        return "isFreeServer";
    }

    public static String countryServers() {
        return "serverList";
    }

    public static String apiNotificationType() {
        return "type";
    }

    public static String serverGroupBannerClass() {
        return "com.protonvpn.android.redesign.countries.ui.ServerGroupUiItem$Banner";
    }

}

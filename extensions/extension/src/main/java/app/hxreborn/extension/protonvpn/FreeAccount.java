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

@SuppressWarnings("unused")
public final class FreeAccount {

    private static volatile boolean signedIn;

    private FreeAccount() {

    }

    public static void onUserInfoChanged(Object userInfo) {
        Object vpnUser = (userInfo != null) ? Reflection.read(userInfo, Members.userInfoVpnUser()) : null;
        signedIn = vpnUser != null && (Boolean) Reflection.call(vpnUser, Members.vpnUserIsFreeUser());
    }

    public static void onUserInfoInvalidated() {
        signedIn = false;
    }

    public static boolean isSignedIn() {
        return signedIn;
    }

}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

@SuppressWarnings("unused")
public final class FreeAccount {

    private static volatile boolean signedIn;

    private FreeAccount() {}

    public static void onUserInfoChanged(Object userInfo) {
        Object vpnUser = userInfo == null ? null : Reflection.call(userInfo, "getVpnUser");
        signedIn = vpnUser != null && (Boolean) Reflection.call(vpnUser, "isFreeUser");
    }

    public static void onUserInfoInvalidated() {
        signedIn = false;
    }

    public static boolean isSignedIn() {
        return signedIn;
    }
}

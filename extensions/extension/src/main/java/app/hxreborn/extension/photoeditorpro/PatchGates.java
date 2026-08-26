/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

public final class PatchGates {

    public static boolean hideAds() {
        return PatchSettings.HIDE_ADS.get();
    }

    public static boolean unlockPremium() {
        return PatchSettings.UNLOCK_PREMIUM.get();
    }

    private PatchGates() {
    }
}

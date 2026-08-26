/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

@SuppressWarnings("unused")
public final class PatchGates {

    private static final int POLL_DIVISOR = 4;
    private static final int UPLOAD_CHUNK_BYTES = 4 * 1024 * 1024;

    public static boolean hideAds() {
        return PatchSettings.HIDE_ADS.get();
    }

    public static boolean unlockPremium() {
        return PatchSettings.UNLOCK_PREMIUM.get();
    }

    public static long pollIntervalMs(long stock) {
        return Math.max(1, stock / POLL_DIVISOR);
    }

    public static int pollBudget(int stock) {
        return stock * POLL_DIVISOR;
    }

    public static int uploadChunkBytes(int stock) {
        return Math.max(stock, UPLOAD_CHUNK_BYTES);
    }

    private PatchGates() {
    }
}

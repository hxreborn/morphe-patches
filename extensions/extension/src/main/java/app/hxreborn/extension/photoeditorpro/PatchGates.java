/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.photoeditorpro;

@SuppressWarnings("unused")
public final class PatchGates {

    private static final int POLL_DIVISOR = 4;

    private static final int UPLOAD_CHUNK_BYTES = 4 * 1024 * 1024;

    private PatchGates() {
    }

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

}

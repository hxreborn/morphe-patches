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
package app.hxreborn.extension.proton;

public final class AmoledTheme {

    private static final String KEY = "amoled_dark_theme";

    private static final String DIM_RAISED_SURFACES_KEY = "amoled_dim_raised_surfaces";

    private static final long BLACK = 0xFF000000L;

    private static final long PACKED_BLACK = BLACK << 32;

    private static final long PACKED_SURFACE = 0xFF2B2B38L << 32;

    private static final long PACKED_DIMMED_SURFACE = 0xFF15151CL << 32;

    private static final int ALPHA_MASK = 0xFF000000;

    private static final int HALF_CHANNELS_MASK = 0x007F7F7F;

    private static final long COLOR_SPACE_MASK = 0xFFFFFFFFL;

    private static final int DARK_CHANNEL_SUM = 3 * 0x80;

    private AmoledTheme() {

    }

    public static boolean isPatched() {
        return false;
    }

    public static boolean isEnabled() {
        return PatchSettings.isFeatureEnabled(isPatched(), KEY);
    }

    static void setEnabled(boolean enabled) {
        PatchSettings.setEnabled(KEY, enabled);
    }

    public static boolean isDimRaisedSurfacesEnabled() {
        return isEnabled() && PatchSettings.isEnabled(DIM_RAISED_SURFACES_KEY, false);
    }

    static void setDimRaisedSurfacesEnabled(boolean enabled) {
        PatchSettings.setEnabled(DIM_RAISED_SURFACES_KEY, enabled);
    }

    public static long transformBackground(long original) {
        return (isEnabled()) ? BLACK : original;
    }

    public static long transformPackedBackground(long original) {
        return (isEnabled() && isDark(original)) ? PACKED_BLACK : original;
    }

    public static long transformPackedSurface(long original) {
        if (!isEnabled() || !isDark(original)) {
            return original;
        }
        return isDimRaisedSurfacesEnabled() ? PACKED_DIMMED_SURFACE : PACKED_SURFACE;
    }

    public static long transformPackedRaisedSurface(long original) {
        if (!isDimRaisedSurfacesEnabled() || !isDark(original)) {
            return original;
        }
        final int argb = (int) (original >>> 32);
        final int halved = (argb & ALPHA_MASK) | ((argb >> 1) & HALF_CHANNELS_MASK);
        return ((long) halved << 32) | (original & COLOR_SPACE_MASK);
    }

    private static boolean isDark(long packedColor) {
        final int argb = (int) (packedColor >>> 32);
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF) < DARK_CHANNEL_SUM;
    }

}

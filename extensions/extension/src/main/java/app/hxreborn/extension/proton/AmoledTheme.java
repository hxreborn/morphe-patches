/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

public final class AmoledTheme {
    private static final String KEY = "amoled_dark_theme";
    private static final long BLACK = 0xFF000000L;
    private static final long PACKED_BLACK = BLACK << 32;
    private static final long PACKED_SURFACE = 0xFF2B2B38L << 32;
    private static final int DARK_CHANNEL_SUM = 3 * 0x80;

    private AmoledTheme() {}

    public static boolean isPatched() {
        return false;
    }

    public static boolean isEnabled() {
        return PatchSettings.isFeatureEnabled(isPatched(), KEY);
    }

    static void setEnabled(boolean enabled) {
        PatchSettings.setEnabled(KEY, enabled);
    }

    public static long transformBackground(long original) {
        return isEnabled() ? BLACK : original;
    }

    public static long transformPackedBackground(long original) {
        return isEnabled() && isDark(original) ? PACKED_BLACK : original;
    }

    public static long transformPackedSurface(long original) {
        return isEnabled() && isDark(original) ? PACKED_SURFACE : original;
    }

    private static boolean isDark(long packedColor) {
        final int argb = (int) (packedColor >>> 32);
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF) < DARK_CHANNEL_SUM;
    }
}

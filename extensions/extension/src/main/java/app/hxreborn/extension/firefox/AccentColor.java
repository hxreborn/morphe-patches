/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.app.Application;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import app.hxreborn.extension.shared.AccentTransform;

@SuppressWarnings("unused")
public final class AccentColor {

    private static final String TAG = "FirefoxPatches";

    private static final String SYSTEM = "system";

    private static final int STOCK_ACCENT = 0xFF764EDD;

    private static final long COLOR_SPACE_MASK = 0x3FL;

    private static volatile boolean resolved;

    private static volatile AccentTransform transform;

    private AccentColor() {
    }

    private static String chosenColor() {
        return "";
    }

    public static long transformColor(long color) {
        AccentTransform accent = transform();
        if (accent == null || (color & COLOR_SPACE_MASK) != 0) {
            return color;
        }
        return ((long) accent.apply((int) (color >>> 32))) << 32;
    }

    static int transformArgb(int argb) {
        AccentTransform accent = transform();
        return (accent != null) ? accent.apply(argb) : argb;
    }

    static int themeAccent(boolean night) {
        return transformArgb(night ? 0xFFC69FFF : 0xFF6B4DFF);
    }

    static boolean hasCustomAccent() {
        return transform() != null;
    }

    public static void installResourceOverlay(Application application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            AccentOverlay.install(application);
        }
    }

    private static AccentTransform transform() {
        if (!resolved) {
            synchronized (AccentColor.class) {
                if (!resolved) {
                    transform = createTransform();
                    resolved = true;
                }
            }
        }
        return transform;
    }

    private static AccentTransform createTransform() {
        try {
            if (!PatchSettings.accentColor()) {
                return null;
            }
            int picked = pickedColor(chosenColor());
            return (picked != 0) ? AccentTransform.between(STOCK_ACCENT, picked) : null;
        } catch (Throwable ex) {
            Log.e(TAG, "accent color failed", ex);
            return null;
        }
    }

    private static int pickedColor(String chosen) throws Exception {
        if (SYSTEM.equals(chosen)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return 0;
            }
            Application app = Reflection.application();
            return (app != null) ? app.getColor(android.R.color.system_accent1_500) : 0;
        }
        try {
            return Color.parseColor(chosen) | 0xFF000000;
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    }

}

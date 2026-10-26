/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Field;

import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

@SuppressWarnings("unused")
public final class AmoledTheme {

    private static final String TAG = "FirefoxPatches";

    private static final long COMPOSE_BLACK = 0xFF000000L << 32;

    private static volatile Field surface;

    private static volatile Field surfaceBright;

    private AmoledTheme() {

    }

    public static void oledConfiguration(Configuration configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PatchSettings.amoledTheme()) {
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_TYPE_MASK)
                    | Configuration.UI_MODE_TYPE_VR_HEADSET;
        }
    }

    public static long pageColor(Object colorScheme) {
        return surfaceColor(colorScheme, false);
    }

    public static long cardColor(Object colorScheme) {
        return surfaceColor(colorScheme, true);
    }

    private static long surfaceColor(Object colorScheme, boolean card) {
        try {
            if (surface == null) {
                surfaceBright = colorScheme.getClass().getField("surfaceBright");
                surface = colorScheme.getClass().getField("surface");
            }
            boolean black = surface.getLong(colorScheme) == COMPOSE_BLACK;
            return ((black == card) ? surfaceBright : surface).getLong(colorScheme);
        } catch (Throwable ex) {
            Log.e(TAG, "surface color failed", ex);
            return 0;
        }
    }

}

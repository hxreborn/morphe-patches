/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.app.Application;
import android.content.om.FabricatedOverlay;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.TypedValue;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
final class AccentOverlay {

    private static final String OVERLAY_NAME = "hx_accent_color";

    private static final String OVERLAYABLE_NAME = "HxAccentColor";

    private static final String ACCENT_COLORS_ARRAY = "hx_accent_colors";

    private AccentOverlay() {
    }

    static void install(Application application) {
        final int accentColors = application.getResources()
            .getIdentifier(ACCENT_COLORS_ARRAY, "array", application.getPackageName());
        if (accentColors == 0) {
            return;
        }
        ResourceOverlay.Entries entries = AccentColor.hasCustomAccent()
                ? (overlay, resources) -> writeEntries(accentColors, overlay, resources) : null;
        ResourceOverlay.install(application, OVERLAY_NAME, OVERLAYABLE_NAME, entries);
    }

    private static void writeEntries(int accentColors, FabricatedOverlay overlay, Resources resources) {
        TypedArray colors = resources.obtainTypedArray(accentColors);
        try {
            for (int index = 0; index < colors.length(); index++) {
                int id = colors.getResourceId(index, 0);
                overlay.setResourceValue(resources.getResourceName(id), TypedValue.TYPE_INT_COLOR_ARGB8,
                        AccentColor.transformArgb(resources.getColor(id, null)), null);
            }
        } finally {
            colors.recycle();
        }
    }

}

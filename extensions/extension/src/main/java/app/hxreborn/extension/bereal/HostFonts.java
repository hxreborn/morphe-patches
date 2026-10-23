/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;

final class HostFonts {

    private static final String TAG = "HostFonts";

    final Typeface regular;

    final Typeface medium;

    final Typeface semibold;

    HostFonts(Context context) {
        this.regular = load(context, "inter_regular", Typeface.DEFAULT);
        this.medium = load(context, "inter_medium", Typeface.DEFAULT);
        this.semibold = load(context, "inter_semibold", Typeface.DEFAULT_BOLD);
    }

    private static Typeface load(Context context, String name, Typeface fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return fallback;
        }
        final int id = context.getResources().getIdentifier(name, "font", context.getPackageName());
        if (id == 0) {
            return fallback;
        }
        try {
            return context.getResources().getFont(id);
        } catch (Resources.NotFoundException ex) {
            Log.e(TAG, "Host font not loaded: " + name, ex);
            return fallback;
        }
    }

}

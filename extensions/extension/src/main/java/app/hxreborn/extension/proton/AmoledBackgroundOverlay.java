/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

@SuppressWarnings("unused")
public final class AmoledBackgroundOverlay {
    private static final String STYLE = "ThemeOverlay.Patches.AmoledBackground";

    private AmoledBackgroundOverlay() {}

    public static boolean isPatched() {
        return false;
    }

    static void apply(Activity activity) {
        if (!isPatched() || !AmoledTheme.isEnabled()) return;

        final int nightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode != Configuration.UI_MODE_NIGHT_YES) return;

        final int style = activity.getResources().getIdentifier(STYLE, "style", activity.getPackageName());
        if (style == 0) return;

        final TypedValue windowBackground = resolveWindowBackground(activity);
        activity.getTheme().applyStyle(style, true);
        final TypedValue overlaidWindowBackground = resolveWindowBackground(activity);
        if (isSameValue(windowBackground, overlaidWindowBackground)) return;

        final TypedArray attributes =
                activity.getTheme().obtainStyledAttributes(new int[] {android.R.attr.windowBackground});
        try {
            final Drawable background = attributes.getDrawable(0);
            if (background != null) activity.getWindow().setBackgroundDrawable(background);
        } finally {
            attributes.recycle();
        }
    }

    private static TypedValue resolveWindowBackground(Activity activity) {
        final TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value;
    }

    private static boolean isSameValue(TypedValue first, TypedValue second) {
        return first.type == second.type && first.data == second.data && first.resourceId == second.resourceId;
    }
}

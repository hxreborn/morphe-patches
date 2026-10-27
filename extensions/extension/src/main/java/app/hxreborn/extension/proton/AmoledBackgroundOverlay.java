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

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

@SuppressWarnings("unused")
public final class AmoledBackgroundOverlay {

    private static final String STYLE = "ThemeOverlay.Patches.AmoledBackground";

    private AmoledBackgroundOverlay() {

    }

    public static boolean isPatched() {
        return false;
    }

    static void apply(Activity activity) {
        if (!isPatched() || !AmoledTheme.isEnabled()) {
            return;
        }

        if (!PatchesTheme.isNightMode(activity)) {
            return;
        }

        final int style = activity.getResources().getIdentifier(STYLE, "style", activity.getPackageName());
        if (style == 0) {
            return;
        }

        final TypedValue windowBackground = resolveWindowBackground(activity);
        activity.getTheme().applyStyle(style, true);
        final TypedValue overlaidWindowBackground = resolveWindowBackground(activity);
        if (isSameValue(windowBackground, overlaidWindowBackground)) {
            return;
        }

        final TypedArray attributes = activity.getTheme()
            .obtainStyledAttributes(new int[] { android.R.attr.windowBackground });
        try {
            final Drawable background = attributes.getDrawable(0);
            if (background != null) {
                activity.getWindow().setBackgroundDrawable(background);
            }
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

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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.RequiresApi;

import app.hxreborn.extension.shared.NightMode;

public final class PatchesTheme {

    private static final int FALLBACK_BACKGROUND_NORM = 0xFF1C1B24;

    private static final int FALLBACK_BACKGROUND_SECONDARY = 0xFF292733;

    private static final int FALLBACK_TEXT_NORM = 0xFFEDEDEE;

    private static final int FALLBACK_TEXT_WEAK = 0xFFA9A9AF;

    private static final int FALLBACK_ICON_DISABLED = 0xFF5B5966;

    private static final int APP_COMPAT_MODE_NIGHT_NO = 1;

    private static final int APP_COMPAT_MODE_NIGHT_YES = 2;

    private static final int APP_COMPAT_MODE_NIGHT_UNSPECIFIED = -100;

    private static final int DARK_COLOR_THRESHOLD = 128;

    private static final int HALF_ALPHA = 0x80;

    private PatchesTheme() {

    }

    public static int backgroundNormAttribute() {
        return 0;
    }

    public static int backgroundSecondaryAttribute() {
        return 0;
    }

    public static int textNormAttribute() {
        return 0;
    }

    public static int textWeakAttribute() {
        return 0;
    }

    public static int iconDisabledAttribute() {
        return 0;
    }

    public static int backArrowDrawable() {
        return 0;
    }

    public static int chevronRightDrawable() {
        return 0;
    }

    public static int checkmarkDrawable() {
        return 0;
    }

    public static int appCompatDefaultNightMode() {
        return APP_COMPAT_MODE_NIGHT_UNSPECIFIED;
    }

    static int uiNightMode() {
        switch (appCompatDefaultNightMode()) {
            case APP_COMPAT_MODE_NIGHT_NO:
                return Configuration.UI_MODE_NIGHT_NO;
            case APP_COMPAT_MODE_NIGHT_YES:
                return Configuration.UI_MODE_NIGHT_YES;
            default:
                return Configuration.UI_MODE_NIGHT_UNDEFINED;
        }
    }

    public static int backgroundNorm(Context context) {
        return resolveColorAttribute(context, backgroundNormAttribute(), FALLBACK_BACKGROUND_NORM);
    }

    public static int backgroundSecondary(Context context) {
        return resolveColorAttribute(context, backgroundSecondaryAttribute(), FALLBACK_BACKGROUND_SECONDARY);
    }

    public static int textNorm(Context context) {
        return resolveColorAttribute(context, textNormAttribute(), FALLBACK_TEXT_NORM);
    }

    public static int textWeak(Context context) {
        return resolveColorAttribute(context, textWeakAttribute(), FALLBACK_TEXT_WEAK);
    }

    static int iconDisabled(Context context) {
        return resolveColorAttribute(context, iconDisabledAttribute(), FALLBACK_ICON_DISABLED);
    }

    static Drawable drawable(Context context, int id) {
        return (id != 0) ? context.getDrawable(id) : null;
    }

    private static int resolveColorAttribute(Context context, int attribute, int fallbackColor) {
        try {
            final TypedValue value = new TypedValue();
            if (!context.getTheme().resolveAttribute(attribute, value, true)) {
                return fallbackColor;
            }
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            return (value.resourceId != 0) ? context.getColor(value.resourceId) : fallbackColor;
        } catch (Throwable ex) {
            return fallbackColor;
        }
    }

    public static GradientDrawable createRoundedRectangle(Context context, int color, int radiusDp) {
        final GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dpToPx(context, radiusDp));
        return background;
    }

    static GradientDrawable createCircle(int color) {
        final GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        return background;
    }

    public static boolean isDark(int color) {
        return weightedRgbBrightness(color) < DARK_COLOR_THRESHOLD;
    }

    private static int weightedRgbBrightness(int color) {
        return (((color >> 16) & 0xFF) * 299 + ((color >> 8) & 0xFF) * 587 + (color & 0xFF) * 114) / 1000;
    }

    public static void tintTextInput(EditText input, int accentColor) {
        final int inactiveColor = iconDisabled(input.getContext());

        input.setBackgroundTintList(new ColorStateList(new int[][] { { android.R.attr.state_focused }, {} },
                new int[] { accentColor, inactiveColor }));
        input.setHighlightColor(withHalfAlpha(accentColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tintTextCursor(input, accentColor);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static void tintTextCursor(EditText input, int accentColor) {
        final Drawable cursor = input.getTextCursorDrawable();
        if (cursor == null) {
            return;
        }

        cursor.setTint(accentColor);
        input.setTextCursorDrawable(cursor);
    }

    static int withHalfAlpha(int color) {
        return (color & 0x00FFFFFF) | (HALF_ALPHA << 24);
    }

    public static void makeClickable(View view) {
        final TypedValue value = new TypedValue();
        if (!view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true)) {
            return;
        }

        view.setClickable(true);
        view.setFocusable(true);
        view.setForeground(view.getContext().getDrawable(value.resourceId));
    }

    public static boolean isNightMode(Context context) {
        return NightMode.isActive(context);
    }

    public static boolean isAppNightMode(Context context) {
        return isNightMode(withNightMode(context));
    }

    public static Context withApplicationTheme(Context context) {
        return new ContextThemeWrapper(withNightMode(context), context.getApplicationInfo().theme);
    }

    static Context withAppNightTheme(Context context) {
        final Context nightContext = withNightMode(context);
        return (nightContext == context) ? context
                : new ContextThemeWrapper(nightContext, context.getApplicationInfo().theme);
    }

    static Context withNightMode(Context base) {
        final int nightMode = uiNightMode();
        final Configuration configuration = base.getResources().getConfiguration();
        final int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_UNDEFINED || nightMode == currentNightMode) {
            return base;
        }

        final Configuration override = new Configuration(configuration);
        override.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        return base.createConfigurationContext(override);
    }

    public static int dpToPx(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

}

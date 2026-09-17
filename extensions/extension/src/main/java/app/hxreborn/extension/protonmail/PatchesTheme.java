/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

final class PatchesTheme {

    static final String BACKGROUND_NORM = "proton_background_norm";
    static final String BACKGROUND_SECONDARY = "proton_background_secondary";
    static final String TEXT_NORM = "proton_text_norm";
    static final String TEXT_WEAK = "proton_text_weak";
    static final String ICON_DISABLED = "proton_icon_disabled";

    private static final int FALLBACK_BACKGROUND_NORM = 0xFF1C1B24;
    private static final int FALLBACK_BACKGROUND_SECONDARY = 0xFF292733;
    private static final int FALLBACK_TEXT_NORM = 0xFFEDEDEE;
    private static final int FALLBACK_TEXT_WEAK = 0xFFA9A9AF;
    private static final int FALLBACK_ICON_DISABLED = 0xFF5B5966;

    private static final int DARK_COLOR_THRESHOLD = 128;
    private static final int HALF_ALPHA = 0x80;

    private PatchesTheme() {}

    static int resolveColorAttribute(Context context, String attribute) {
        final int fallbackColor = fallbackColor(attribute);
        try {
            final int identifier = context.getResources()
                    .getIdentifier(attribute, "attr", context.getPackageName());
            if (identifier == 0) return fallbackColor;

            final TypedValue value = new TypedValue();
            if (!context.getTheme().resolveAttribute(identifier, value, true)) return fallbackColor;
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            return value.resourceId == 0 ? fallbackColor : context.getColor(value.resourceId);
        } catch (Throwable t) {
            return fallbackColor;
        }
    }

    private static int fallbackColor(String attribute) {
        switch (attribute) {
            case BACKGROUND_NORM:
                return FALLBACK_BACKGROUND_NORM;
            case TEXT_NORM:
                return FALLBACK_TEXT_NORM;
            case TEXT_WEAK:
                return FALLBACK_TEXT_WEAK;
            case ICON_DISABLED:
                return FALLBACK_ICON_DISABLED;
            default:
                return FALLBACK_BACKGROUND_SECONDARY;
        }
    }

    static Drawable getDrawableByName(Context context, String name) {
        final int identifier = context.getResources()
                .getIdentifier(name, "drawable", context.getPackageName());
        return identifier == 0 ? null : context.getDrawable(identifier);
    }

    static GradientDrawable createRoundedRectangle(Context context, int color, int radiusDp) {
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

    static boolean isDark(int color) {
        return weightedRgbBrightness(color) < DARK_COLOR_THRESHOLD;
    }

    private static int weightedRgbBrightness(int color) {
        return (((color >> 16) & 0xFF) * 299
                + ((color >> 8) & 0xFF) * 587
                + (color & 0xFF) * 114) / 1000;
    }

    static void tintSwitch(Switch control, int accentColor) {
        final int inactiveColor = resolveColorAttribute(control.getContext(), ICON_DISABLED);
        final int[][] states = {{android.R.attr.state_checked}, {}};

        control.setThumbTintList(
                new ColorStateList(states, new int[] {accentColor, inactiveColor}));
        control.setTrackTintList(
                new ColorStateList(states, new int[] {withHalfAlpha(accentColor), inactiveColor}));
    }

    static void tintTextInput(EditText input, int accentColor) {
        final int inactiveColor = resolveColorAttribute(input.getContext(), ICON_DISABLED);

        input.setBackgroundTintList(new ColorStateList(
                new int[][] {{android.R.attr.state_focused}, {}},
                new int[] {accentColor, inactiveColor}));
        input.setHighlightColor(withHalfAlpha(accentColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tintTextCursor(input, accentColor);
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private static void tintTextCursor(EditText input, int accentColor) {
        final Drawable cursor = input.getTextCursorDrawable();
        if (cursor == null) return;

        cursor.setTint(accentColor);
        input.setTextCursorDrawable(cursor);
    }

    static int withHalfAlpha(int color) {
        return (color & 0x00FFFFFF) | (HALF_ALPHA << 24);
    }

    static void makeClickable(View view) {
        final TypedValue value = new TypedValue();
        if (!view.getContext().getTheme()
                .resolveAttribute(android.R.attr.selectableItemBackground, value, true)) {
            return;
        }

        view.setClickable(true);
        view.setFocusable(true);
        view.setForeground(view.getContext().getDrawable(value.resourceId));
    }

    static int dpToPx(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

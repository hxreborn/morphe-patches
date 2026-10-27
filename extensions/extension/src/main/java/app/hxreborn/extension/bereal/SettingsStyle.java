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
package app.hxreborn.extension.bereal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

final class SettingsStyle {

    static final int SCREEN_BACKGROUND = Color.BLACK;

    static final int CARD_SURFACE = 0xFF1C1C1E;

    static final int FIELD_SURFACE = 0xFF2C2C2E;

    static final int DIVIDER = 0xFF2C2C2E;

    static final int PRIMARY_TEXT = Color.WHITE;

    static final int SECONDARY_TEXT = 0xFF99999C;

    static final int ACCENT = 0xFF0A84FF;

    static final int RIPPLE = 0x33FFFFFF;

    static final int CAUTION = 0xFFFF9F0A;

    static final int CAUTION_SURFACE = 0x33FF9F0A;

    static final int MAP_CONTROL_SURFACE = 0xE61C1C1E;

    static final float BAR_TITLE_SP = 17f;

    static final float TITLE_SP = 20f;

    static final float ROW_TITLE_SP = 16f;

    static final float BODY_SP = 14f;

    static final float SECTION_SP = 13f;

    static final float CHIP_SP = 12f;

    static final int SCREEN_INSET_DP = 16;

    static final int CARD_RADIUS_DP = 16;

    static final int FIELD_RADIUS_DP = 12;

    static final int ICON_DP = 24;

    static final int CHIP_RADIUS_DP = 8;

    static final int CONTENT_MAX_DP = 640;

    private static final String TAG = "SettingsStyle";

    final HostFonts fonts;

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.fonts = new HostFonts(context);
    }

    int dp(float value) {
        return Dimensions.dp(this.context, value);
    }

    TextView text(String value, float sizeSp, int color, Typeface typeface) {
        final TextView view = new TextView(this.context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        return view;
    }

    TextView chip(String label) {
        final TextView chip = text(label, CHIP_SP, CAUTION, this.fonts.medium);
        chip.setSingleLine(true);
        chip.setIncludeFontPadding(false);
        chip.setPadding(dp(8), dp(3), dp(8), dp(3));
        chip.setBackground(rounded(CAUTION_SURFACE, CHIP_RADIUS_DP));
        return chip;
    }

    GradientDrawable rounded(int color, int radiusDp) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    Drawable ripple(int surface, int radiusDp) {
        final GradientDrawable mask = rounded(Color.WHITE, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(RIPPLE), rounded(surface, radiusDp), mask);
    }

    static Drawable hostIcon(Context context, String name) {
        final int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id == 0) {
            return null;
        }
        try {
            final Drawable icon = context.getDrawable(id);
            if (icon == null) {
                return null;
            }
            final Drawable tinted = icon.mutate();
            tinted.setColorFilter(PRIMARY_TEXT, PorterDuff.Mode.SRC_IN);
            return tinted;
        } catch (Resources.NotFoundException ex) {
            Log.e(TAG, "Host icon not loaded: " + name, ex);
            return null;
        }
    }

    ImageView hostIconView(String name) {
        final Drawable icon = hostIcon(this.context, name);
        if (icon == null) {
            return null;
        }
        final ImageView view = new ImageView(this.context);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setImageDrawable(icon);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

}

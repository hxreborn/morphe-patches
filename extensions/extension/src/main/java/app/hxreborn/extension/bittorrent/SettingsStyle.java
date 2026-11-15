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
package app.hxreborn.extension.bittorrent;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

final class SettingsStyle {

    static final float BAR_TITLE_SP = 24f;
    static final float SECTION_SP = 14f;
    static final float ROW_TITLE_SP = 17f;
    static final float BODY_SP = 14f;
    static final int SCREEN_INSET_DP = 16;
    static final int CARD_RADIUS_DP = 16;
    static final int ICON_DP = 24;

    private static final int DARK_RIPPLE = 0x33FFFFFF;

    private static final int LIGHT_RIPPLE = 0x1F000000;

    private static final int LIGHT_CARD_SURFACE = 0xFFF3F3F3;

    private static final int DARK_CARD_SURFACE = 0xFF1C1C1E;

    final boolean dark;

    final int screenBackground;

    final int cardSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final int accent;

    final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.dark = PatchSettings.isDark(context);
        this.screenBackground = this.dark ? color(darkBackgroundId()) : Color.WHITE;
        this.cardSurface = this.dark ? DARK_CARD_SURFACE : LIGHT_CARD_SURFACE;
        this.divider = color(this.dark ? darkDividerId() : lightDividerId());
        this.primaryText = color(this.dark ? darkTitleId() : lightTitleId());
        this.secondaryText = color(this.dark ? darkLabelId() : lightLabelId());
        this.accent = color(this.dark ? darkAccentId() : lightAccentId());
    }

    int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                this.context.getResources().getDisplayMetrics()));
    }

    TextView text(String value, float sizeSp, int color, Typeface typeface) {
        TextView view = new TextView(this.context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        return view;
    }

    GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    Drawable ripple(int radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(this.dark ? DARK_RIPPLE : LIGHT_RIPPLE), null,
                rounded(Color.WHITE, radiusDp));
    }

    Drawable hostIcon(int identifier, int tint) {
        Drawable icon = this.context.getDrawable(identifier).mutate();
        icon.setTint(tint);
        return icon;
    }

    Drawable backIcon(int tint) {
        return hostIcon(backIconId(), tint);
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private int color(int identifier) {
        return this.context.getColor(identifier);
    }

    static int themeIconId() {
        return 0;
    }

    static int resetIconId() {
        return 0;
    }

    static int infoIconId() {
        return 0;
    }

    static int patchesIconId() {
        return 0;
    }

    static int backIconId() {
        return 0;
    }

    static int darkBackgroundId() {
        return 0;
    }

    static int darkDividerId() {
        return 0;
    }

    static int lightDividerId() {
        return 0;
    }

    static int darkTitleId() {
        return 0;
    }

    static int lightTitleId() {
        return 0;
    }

    static int darkLabelId() {
        return 0;
    }

    static int lightLabelId() {
        return 0;
    }

    static int darkAccentId() {
        return 0;
    }

    static int lightAccentId() {
        return 0;
    }

}

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
package app.hxreborn.extension.readera;

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
import android.widget.Switch;
import android.widget.TextView;

final class SettingsStyle {

    static final float BAR_TITLE_SP = 20f;

    static final float SECTION_SP = 14f;

    static final float ROW_TITLE_SP = 16f;

    static final float BODY_SP = 13f;

    static final int SCREEN_INSET_DP = 12;

    static final int CARD_RADIUS_DP = 8;

    static final int ICON_DP = 24;

    private static final int SWITCH_WIDTH_DP = 52;

    private static final int TOUCH_TARGET_DP = 48;

    private static final int APP_BAR_BACKGROUND = 0xFF005C75;

    private static final int SCREEN_BACKGROUND = 0xFF051E25;

    private static final int CARD_SURFACE = 0xFF1C3239;

    private static final int MENU_SURFACE = 0xFF2A4149;

    private static final int DIVIDER = 0x1FFFFFFF;

    private static final int SECONDARY_TEXT = 0xFFB5BCBE;

    private static final int RIPPLE = 0x33FFFFFF;

    private static final int AMOLED_APP_BAR_BACKGROUND = 0xFF0B0B0B;

    private static final int AMOLED_SCREEN_BACKGROUND = Color.BLACK;

    private static final int AMOLED_CARD_SURFACE = 0xFF171717;

    private static final int AMOLED_MENU_SURFACE = 0xFF262626;

    private static final int AMOLED_SECONDARY_TEXT = 0xFFBDBDBD;

    final int appBarBackground;

    final int screenBackground;

    final int cardSurface;

    final int menuSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        boolean amoled = AppTheme.amoledApplied();
        this.appBarBackground = amoled ? AMOLED_APP_BAR_BACKGROUND : APP_BAR_BACKGROUND;
        this.screenBackground = amoled ? AMOLED_SCREEN_BACKGROUND : SCREEN_BACKGROUND;
        this.cardSurface = amoled ? AMOLED_CARD_SURFACE : CARD_SURFACE;
        this.menuSurface = amoled ? AMOLED_MENU_SURFACE : MENU_SURFACE;
        this.divider = DIVIDER;
        this.primaryText = Color.WHITE;
        this.secondaryText = amoled ? AMOLED_SECONDARY_TEXT : SECONDARY_TEXT;
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
        return new RippleDrawable(ColorStateList.valueOf(RIPPLE), null, rounded(Color.WHITE, radiusDp));
    }

    Drawable icon(String name, int tint) {
        Drawable icon = ownedDrawable(name);
        icon.setTint(tint);
        return icon;
    }

    Drawable backIcon(int tint) {
        return icon("back", tint);
    }

    void styleSwitch(Switch control) {
        control.setThumbDrawable(ownedDrawable("switch_thumb"));
        control.setTrackDrawable(ownedDrawable("switch_track"));
        control.setSwitchMinWidth(dp(SWITCH_WIDTH_DP));
        control.setMinimumHeight(dp(TOUCH_TARGET_DP));
        control.setThumbTintList(null);
        control.setTrackTintList(null);
        control.setSplitTrack(false);
        control.refreshDrawableState();
    }

    private Drawable ownedDrawable(String name) {
        int resource = this.context.getResources()
            .getIdentifier("hx_readera_" + name.toLowerCase(java.util.Locale.ROOT), "drawable",
                    this.context.getPackageName());
        return this.context.getDrawable(resource).mutate();
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

}

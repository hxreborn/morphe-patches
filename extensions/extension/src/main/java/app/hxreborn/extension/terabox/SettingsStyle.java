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
package app.hxreborn.extension.terabox;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

final class SettingsStyle {

    static final float BAR_TITLE_SP = 24f;

    static final float SECTION_SP = 14f;

    static final float ROW_TITLE_SP = 17f;

    static final float BODY_SP = 14f;

    static final int SCREEN_INSET_DP = 16;

    static final int CARD_RADIUS_DP = 16;

    static final int ICON_DP = 24;

    final int screenBackground;

    final int cardSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final Typeface medium;

    private final int ripple;

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.screenBackground = context.getColor(pageBackgroundColor());
        this.cardSurface = context.getColor(cardBackgroundColor());
        this.divider = context.getColor(dividerColor());
        this.primaryText = context.getColor(primaryTextColor());
        this.secondaryText = context.getColor(secondaryTextColor());
        this.medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        this.ripple = context.getColor(dividerColor());
    }

    static int pageBackgroundColor() {
        return 0;
    }

    static int cardBackgroundColor() {
        return 0;
    }

    static int dividerColor() {
        return 0;
    }

    static int primaryTextColor() {
        return 0;
    }

    static int secondaryTextColor() {
        return 0;
    }

    static int switchButtonDrawable() {
        return 0;
    }

    static int backIconDrawable() {
        return 0;
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
        if (typeface != null) {
            view.setTypeface(typeface);
        }
        return view;
    }

    GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    Drawable ripple(int radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(this.ripple), null, rounded(this.screenBackground, radiusDp));
    }

    Drawable hostIcon(String name, int tint) {
        return tinted(hostDrawable(name), tint);
    }

    Drawable backIcon(int tint) {
        return tinted(hostDrawable(backIconDrawable()), tint);
    }

    void styleSwitch(CheckBox control) {
        Drawable button = hostDrawable(switchButtonDrawable());
        if (button != null) {
            control.setButtonDrawable(button);
        }
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private static Drawable tinted(Drawable icon, int tint) {
        if (icon != null) {
            icon.setTint(tint);
        }
        return icon;
    }

    private Drawable hostDrawable(String name) {
        int identifier = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        return (identifier != 0) ? hostDrawable(identifier) : null;
    }

    private Drawable hostDrawable(int identifier) {
        Drawable drawable = this.context.getDrawable(identifier);
        return (drawable != null) ? drawable.mutate() : null;
    }

}

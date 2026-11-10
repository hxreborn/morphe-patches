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
package app.hxreborn.extension.vpnsuper;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
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

    static final float BAR_TITLE_SP = 24f;

    static final float SECTION_SP = 14f;

    static final float ROW_TITLE_SP = 17f;

    static final float BODY_SP = 14f;

    static final int SCREEN_INSET_DP = 16;

    static final int CARD_RADIUS_DP = 12;

    static final int ICON_DP = 24;

    final int screenBackground;

    final int cardSurface;

    final int primaryText;

    final int secondaryText;

    final Typeface regular;

    final Typeface medium;

    private final int switchOn;

    private final int switchOff;

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.screenBackground = context.getColor(screenBackgroundColor());
        this.cardSurface = context.getColor(cardBackgroundColor());
        this.primaryText = context.getColor(primaryTextColor());
        this.secondaryText = context.getColor(secondaryTextColor());
        this.switchOn = context.getColor(switchOnColor());
        this.switchOff = context.getColor(switchOffColor());
        this.regular = hostFont(regularFont(), Typeface.DEFAULT);
        this.medium = hostFont(mediumFont(), Typeface.DEFAULT_BOLD);
    }

    static int screenBackgroundColor() {
        return 0;
    }

    static int cardBackgroundColor() {
        return 0;
    }

    static int primaryTextColor() {
        return 0;
    }

    static int secondaryTextColor() {
        return 0;
    }

    static int switchOnColor() {
        return 0;
    }

    static int switchOffColor() {
        return 0;
    }

    static int regularFont() {
        return 0;
    }

    static int mediumFont() {
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
        return new RippleDrawable(ColorStateList.valueOf(this.screenBackground), null,
                rounded(this.cardSurface, radiusDp));
    }

    Drawable icon(String name) {
        int identifier = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        return (identifier != 0) ? tinted(identifier) : null;
    }

    Drawable backIcon() {
        return tinted(backIconDrawable());
    }

    void styleSwitch(Switch control) {
        control.setThumbTintList(checkedTint(this.switchOn, this.switchOff));
        control.setTrackTintList(checkedTint(this.switchOn, this.secondaryText));
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private Drawable tinted(int identifier) {
        Drawable drawable = this.context.getDrawable(identifier);
        if (drawable == null) {
            return null;
        }
        drawable = drawable.mutate();
        drawable.setTint(this.primaryText);
        return drawable;
    }

    private Typeface hostFont(int identifier, Typeface fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return fallback;
        }
        return font(identifier);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private Typeface font(int identifier) {
        return this.context.getResources().getFont(identifier);
    }

    private static ColorStateList checkedTint(int checked, int unchecked) {
        return new ColorStateList(new int[][] { { android.R.attr.state_checked }, {} },
                new int[] { checked, unchecked });
    }

}

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
package app.hxreborn.extension.viber;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.widget.TextView;

final class SettingsStyle extends app.hxreborn.extension.shared.patchesmenu.SettingsStyle {

    private static final int DIVIDER_ALPHA = 0x1F;

    private final Context context;

    SettingsStyle(Context context) {
        super(context, isDarknightTheme(), color(context, mainBackgroundAttribute()),
                color(context, secondaryBackgroundAttribute()), divider(context),
                color(context, textPrimaryAttribute()), color(context, textSecondaryAttribute()),
                color(context, textPrimaryAttribute()), divider(context),
                Typeface.create(new TextView(context).getTypeface(), Typeface.BOLD));
        this.context = context;
    }

    private static int divider(Context context) {
        int primary = color(context, textPrimaryAttribute());
        return Color.argb(DIVIDER_ALPHA, Color.red(primary), Color.green(primary), Color.blue(primary));
    }

    static int lightTheme() {
        return 0;
    }

    static int darknightTheme() {
        return 0;
    }

    static int mainBackgroundAttribute() {
        return 0;
    }

    static int secondaryBackgroundAttribute() {
        return 0;
    }

    static int textPrimaryAttribute() {
        return 0;
    }

    static int textSecondaryAttribute() {
        return 0;
    }

    static boolean isDarknightTheme() {
        return false;
    }

    static int theme() {
        return isDarknightTheme() ? darknightTheme() : lightTheme();
    }

    private static int color(Context context, int attribute) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attribute, value, true);
        return (value.resourceId != 0) ? context.getColor(value.resourceId) : value.data;
    }

    @Override
    public int dp(float value) {
        return Math.round(value * this.context.getResources().getDisplayMetrics().density);
    }

    @Override
    public TextView text(String value, float size, int color, Typeface typeface) {
        TextView text = new TextView(this.context);
        text.setText(value);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        text.setTextColor(color);
        if (typeface != null) {
            text.setTypeface(typeface);
        }
        return text;
    }

    @Override
    public GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    @Override
    public Drawable ripple(int radius) {
        return new RippleDrawable(ColorStateList.valueOf(this.divider), null, rounded(this.primaryText, radius));
    }

    @SuppressLint("DiscouragedApi")
    @SuppressWarnings("DataFlowIssue")
    Drawable hostIcon(String name, int tint) {
        int id = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        Drawable drawable = this.context.getDrawable(id).mutate();
        drawable.setTint(tint);
        return drawable;
    }

    @Override
    public Drawable backIcon(int tint) {
        return hostIcon("hx_viber_ic_back", tint);
    }

    @Override
    protected int backIconResource() {
        return this.context.getResources().getIdentifier("hx_viber_ic_back", "drawable", this.context.getPackageName());
    }

}

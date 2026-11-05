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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
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

    private static final String HOST_PREFERENCES = "com.viber.voip.ViberPrefs";

    private static final String HOST_THEME = "current_theme";

    private static final int DIVIDER_ALPHA = 0x1F;

    final boolean dark;

    final int screenBackground;

    final int cardSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final Typeface medium;

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.screenBackground = color("mainBackgroundColor");
        this.cardSurface = color("secondaryBackgroundColor");
        this.primaryText = color("textPrimaryColor");
        this.secondaryText = color("textSecondaryColor");
        this.divider = Color.argb(DIVIDER_ALPHA, Color.red(this.primaryText), Color.green(this.primaryText),
                Color.blue(this.primaryText));
        this.dark = "darknight".equals(themeName(context));
        this.medium = Typeface.create(new TextView(context).getTypeface(), Typeface.BOLD);
    }

    static int theme(Context context) {
        String name = "darknight".equals(themeName(context)) ? "Darknight.Theme.Viber.NoActionBar"
                : "Theme.Viber.NoActionBar";
        return context.getResources().getIdentifier(name, "style", context.getPackageName());
    }

    private static String themeName(Context context) {
        boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return context.getSharedPreferences(HOST_PREFERENCES, Context.MODE_PRIVATE)
            .getString(HOST_THEME, night ? "darknight" : "light");
    }

    private int color(String name) {
        int id = this.context.getResources().getIdentifier(name, "attr", this.context.getPackageName());
        TypedValue value = new TypedValue();
        this.context.getTheme().resolveAttribute(id, value, true);
        return (value.resourceId != 0) ? this.context.getColor(value.resourceId) : value.data;
    }

    int dp(float value) {
        return Math.round(value * this.context.getResources().getDisplayMetrics().density);
    }

    TextView text(String value, float size, int color, Typeface typeface) {
        TextView text = new TextView(this.context);
        text.setText(value);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        text.setTextColor(color);
        if (typeface != null) {
            text.setTypeface(typeface);
        }
        return text;
    }

    GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    Drawable ripple(int radius) {
        return new RippleDrawable(ColorStateList.valueOf(this.divider), null, rounded(this.primaryText, radius));
    }

    Drawable hostIcon(String name, int tint) {
        int id = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        Drawable drawable = this.context.getDrawable(id).mutate();
        drawable.setTint(tint);
        return drawable;
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

}

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
package app.hxreborn.extension.shared.patchesmenu;

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

public abstract class SettingsStyle {

    public static final float BAR_TITLE_SP = 24f;

    public static final float SECTION_SP = 14f;

    public static final float ROW_TITLE_SP = 17f;

    public static final float BODY_SP = 14f;

    public static final int SCREEN_INSET_DP = 16;

    public static final int CARD_RADIUS_DP = 16;

    public static final int ICON_DP = 24;

    private static final int DARK_RIPPLE = 0x33FFFFFF;

    private static final int LIGHT_RIPPLE = 0x1F000000;

    private static final int LIGHT_CARD_SURFACE = 0xFFF3F3F3;

    private static final int DARK_CARD_SURFACE = 0xFF1C1C1E;

    public final boolean dark;

    public final int screenBackground;

    public final int cardSurface;

    public final int divider;

    public final int primaryText;

    public final int secondaryText;

    public final int accent;

    public final Typeface medium;

    private final int rippleColor;

    private final Context context;

    protected SettingsStyle(Context context, boolean dark, int backgroundId, int dividerId, int titleId, int labelId,
            int accentId) {
        this.context = context;
        this.medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        this.rippleColor = dark ? DARK_RIPPLE : LIGHT_RIPPLE;
        this.dark = dark;
        this.screenBackground = this.dark ? color(backgroundId) : Color.WHITE;
        this.cardSurface = this.dark ? DARK_CARD_SURFACE : LIGHT_CARD_SURFACE;
        this.divider = color(dividerId);
        this.primaryText = color(titleId);
        this.secondaryText = color(labelId);
        this.accent = color(accentId);
    }

    protected SettingsStyle(Context context, boolean dark, int screenBackground, int cardSurface, int divider,
            int primaryText, int secondaryText, int accent, int rippleColor, Typeface medium) {
        this.context = context;
        this.dark = dark;
        this.screenBackground = screenBackground;
        this.cardSurface = cardSurface;
        this.divider = divider;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.accent = accent;
        this.rippleColor = rippleColor;
        this.medium = medium;
    }

    public int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                this.context.getResources().getDisplayMetrics()));
    }

    public TextView text(String value, float sizeSp, int color, Typeface typeface) {
        TextView view = new TextView(this.context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        return view;
    }

    public GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    public Drawable ripple(int radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(this.rippleColor), null, rounded(Color.WHITE, radiusDp));
    }

    @SuppressWarnings("DataFlowIssue")
    public Drawable hostIcon(int identifier, int tint) {
        Drawable icon = this.context.getDrawable(identifier).mutate();
        icon.setTint(tint);
        return icon;
    }

    public Drawable backIcon(int tint) {
        return hostIcon(backIconResource(), tint);
    }

    public static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private int color(int identifier) {
        return this.context.getColor(identifier);
    }

    protected abstract int backIconResource();

}

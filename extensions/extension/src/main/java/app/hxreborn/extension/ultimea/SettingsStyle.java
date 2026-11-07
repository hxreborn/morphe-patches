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
package app.hxreborn.extension.ultimea;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
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

    static final int CARD_RADIUS_DP = 16;

    static final int ICON_DP = 24;

    private static final int DARK_SCREEN_BACKGROUND = 0xFF121212;

    private static final int DARK_CARD_SURFACE = 0xFF1E1E1E;

    private static final int DARK_DIVIDER = 0xFF2C2C2C;

    private static final int LIGHT_DIVIDER = 0xFFE2E2E2;

    private static final int LIGHT_PRIMARY_TEXT = 0xFF121212;

    private static final int DARK_SECONDARY_TEXT = 0xFFBFBFBF;

    private static final int LIGHT_SECONDARY_TEXT = 0xFF5C5C5C;

    private static final int OPAQUE_ALPHA = 255;

    private static final int DARK_RIPPLE = 0x33FFFFFF;

    private static final int LIGHT_RIPPLE = 0x1F000000;

    private static final int LIGHT_SCREEN_BACKGROUND = 0xFFF6F6F6;

    private static final int ACCENT = 0xFF541597;

    final boolean dark;

    final int screenBackground;

    final int cardSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private final int ripple;

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.dark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        this.screenBackground = this.dark ? DARK_SCREEN_BACKGROUND : LIGHT_SCREEN_BACKGROUND;
        this.cardSurface = this.dark ? DARK_CARD_SURFACE : Color.WHITE;
        this.divider = this.dark ? DARK_DIVIDER : LIGHT_DIVIDER;
        this.primaryText = this.dark ? Color.WHITE : LIGHT_PRIMARY_TEXT;
        this.secondaryText = this.dark ? DARK_SECONDARY_TEXT : LIGHT_SECONDARY_TEXT;
        this.ripple = this.dark ? DARK_RIPPLE : LIGHT_RIPPLE;
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
        return new RippleDrawable(ColorStateList.valueOf(this.ripple), null, rounded(Color.WHITE, radiusDp));
    }

    Drawable hostIcon(String name, int tint) {
        int identifier = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        Drawable icon = hostDrawable(identifier);
        if (icon != null) {
            icon.setTint(tint);
        }
        return icon;
    }

    Drawable backIcon(int tint) {
        Drawable icon = hostIcon("hx_ultimea_ic_back", tint);
        icon.setTint(tint);
        return icon;
    }

    void styleSwitch(Switch control) {
        SwitchColors colors = new SwitchColors(ACCENT, this.cardSurface, this.dark);
        control.setThumbTintMode(PorterDuff.Mode.SRC_IN);
        control.setThumbTintList(ColorStateList.valueOf(colors.thumb));
        control.setTrackDrawable(switchTrack(control.getTrackDrawable(), colors));
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private Drawable hostDrawable(int identifier) {
        Drawable drawable = this.context.getDrawable(identifier);
        return (drawable != null) ? drawable.mutate() : null;
    }

    private Drawable switchTrack(Drawable stock, SwitchColors colors) {
        StateListDrawable track = new StateListDrawable();
        track.addState(new int[] { android.R.attr.state_checked }, coloredTrack(stock, colors.checkedTrack));
        track.addState(new int[] {}, coloredTrack(stock, colors.uncheckedTrack));
        return track;
    }

    private Drawable coloredTrack(Drawable stock, int color) {
        Drawable track = stock.getConstantState().newDrawable(this.context.getResources()).mutate();
        setTrackColor(track, color);
        return track;
    }

    private static void setTrackColor(Drawable drawable, int color) {
        drawable.setTintList(null);
        drawable.setAlpha(OPAQUE_ALPHA);
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layers = (LayerDrawable) drawable;
            for (int index = 0; index < layers.getNumberOfLayers(); index++) {
                setTrackColor(layers.getDrawable(index), color);
            }
        } else if (drawable instanceof DrawableWrapper) {
            setTrackColor(((DrawableWrapper) drawable).getDrawable(), color);
        } else if (drawable instanceof GradientDrawable) {
            ((GradientDrawable) drawable).setColor(color);
        } else {
            drawable.setTint(color);
        }
    }

}

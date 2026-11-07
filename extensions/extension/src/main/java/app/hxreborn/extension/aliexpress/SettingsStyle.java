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
package app.hxreborn.extension.aliexpress;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

@SuppressWarnings("unused")
final class SettingsStyle {

    static final float BAR_TITLE_SP = 24f;

    static final float SECTION_SP = 14f;

    static final float ROW_TITLE_SP = 17f;

    static final float BODY_SP = 14f;

    static final int SCREEN_INSET_DP = 16;

    static final int CARD_RADIUS_DP = 16;

    static final int ICON_DP = 24;

    private static final int CHANNEL_COUNT = 3;

    private static final int MID_CHANNEL = 0x80;

    private static final int OPAQUE_ALPHA = 0xFF;

    private static final int SECONDARY_TEXT_ALPHA = 0xB3;

    private static final int DIVIDER_ALPHA = 0x1F;

    private static final float DARK_PAGE_BRIGHTNESS = 0.6f;

    final Context context;

    final int screenBackground;

    final int cardSurface;

    final int divider;

    final int primaryText;

    final int secondaryText;

    final Typeface medium;

    final int accent;

    final boolean lightScreen;

    private final Typeface regular;

    private final int rippleColor;

    SettingsStyle(Context context, TextView nativeTitle) {
        this.context = context;
        this.primaryText = nativeTitle.getCurrentTextColor();
        this.regular = nativeTitle.getTypeface();
        this.medium = Typeface.create(this.regular, Typeface.BOLD);
        View rowSurface = opaqueBackgroundOwner(nativeTitle);
        View pageSurface = (rowSurface != null) ? opaqueBackgroundOwner(parentView(rowSurface)) : null;
        int row = (rowSurface != null) ? opaqueColor(rowSurface) : color(whiteColor());
        this.lightScreen = isLight(row);
        int page = (pageSurface != null) ? opaqueColor(pageSurface) : row;
        if (page == row) {
            page = this.lightScreen ? color(lightGrayColor()) : darker(row);
        }
        this.screenBackground = this.lightScreen ? row : page;
        this.cardSurface = this.lightScreen ? page : row;
        this.secondaryText = withAlpha(this.primaryText, SECONDARY_TEXT_ALPHA);
        this.divider = withAlpha(this.primaryText, DIVIDER_ALPHA);
        this.rippleColor = themeColor(android.R.attr.colorControlHighlight, this.secondaryText);
        this.accent = color(brandColor());
    }

    static int whiteColor() {
        return 0;
    }

    static int lightGrayColor() {
        return 0;
    }

    static int brandColor() {
        return 0;
    }

    int dp(float value) {
        return Dimensions.dp(this.context, value);
    }

    TextView text(String value, float sizeSp, int color, Typeface typeface) {
        TextView view = new TextView(this.context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface((typeface != null) ? typeface : this.regular);
        return view;
    }

    GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    Drawable ripple(int radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(this.rippleColor), null, rounded(this.primaryText, radiusDp));
    }

    Drawable hostIcon(String name, int tint) {
        int identifier = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        if (identifier == 0) {
            return null;
        }
        Drawable icon = this.context.getDrawable(identifier).mutate();
        icon.setTint(tint);
        return icon;
    }

    static CompoundButton createSwitch(Context context) {
        return null;
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    private static View parentView(View view) {
        ViewParent parent = view.getParent();
        return (parent instanceof View) ? (View) parent : null;
    }

    private static View opaqueBackgroundOwner(View start) {
        View view = start;
        while (view != null) {
            if (view.getBackground() instanceof ColorDrawable && Color.alpha(opaqueColor(view)) == OPAQUE_ALPHA) {
                return view;
            }
            view = parentView(view);
        }
        return null;
    }

    private static int opaqueColor(View view) {
        return ((ColorDrawable) view.getBackground()).getColor();
    }

    private static int darker(int color) {
        return Color.rgb((int) (Color.red(color) * DARK_PAGE_BRIGHTNESS),
                (int) (Color.green(color) * DARK_PAGE_BRIGHTNESS), (int) (Color.blue(color) * DARK_PAGE_BRIGHTNESS));
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static boolean isLight(int color) {
        return (Color.red(color) + Color.green(color) + Color.blue(color)) / CHANNEL_COUNT > MID_CHANNEL;
    }

    private int themeColor(int attribute, int fallback) {
        TypedArray theme = this.context.obtainStyledAttributes(new int[] { attribute });
        int color = theme.getColor(0, fallback);
        theme.recycle();
        return color;
    }

    @SuppressWarnings("deprecation")
    private int color(int id) {
        return this.context.getResources().getColor(id);
    }

}

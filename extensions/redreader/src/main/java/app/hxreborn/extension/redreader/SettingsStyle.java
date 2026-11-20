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
package app.hxreborn.extension.redreader;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.widget.TextView;

final class SettingsStyle {

    static final int INSET_DP = 16;

    static final int ROW_HEIGHT_DP = 72;

    static final int TOUCH_TARGET_DP = 48;

    static final int CARD_RADIUS_DP = 12;

    static final float TITLE_SP = 20;

    static final float BODY_SP = 16;

    static final float SUMMARY_SP = 14;

    private static final int SURFACE_BRIGHTNESS_THRESHOLD = 384;

    private static final int SURFACE_CHANNEL_OFFSET = 12;

    private static final int RIPPLE_ALPHA_MASK = 0x33FFFFFF;

    private static final int MAX_CHANNEL = 255;

    final int text;

    final int secondary;

    final int background;

    private final Context context;

    SettingsStyle(Context context) {
        this.context = context;
        this.text = color(android.R.attr.textColorPrimary);
        this.secondary = color(android.R.attr.textColorSecondary);
        this.background = color(android.R.attr.colorBackground);
    }

    static int backIconDrawable() {
        return 0;
    }

    Drawable backIcon() {
        Drawable icon = this.context.getResources().getDrawable(backIconDrawable()).mutate();
        icon.setTint(this.text);
        return icon;
    }

    int dp(int size) {
        return Math.round(size * this.context.getResources().getDisplayMetrics().density);
    }

    TextView text(String value, float size, int color) {
        TextView view = new TextView(this.context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    GradientDrawable surface() {
        GradientDrawable drawable = new GradientDrawable();
        int channelOffset = (Color.red(this.background) + Color.green(this.background)
                + Color.blue(this.background) < SURFACE_BRIGHTNESS_THRESHOLD) ? SURFACE_CHANNEL_OFFSET
                        : -SURFACE_CHANNEL_OFFSET;
        drawable.setColor(Color.rgb(channel(Color.red(this.background) + channelOffset),
                channel(Color.green(this.background) + channelOffset),
                channel(Color.blue(this.background) + channelOffset)));
        drawable.setCornerRadius(dp(CARD_RADIUS_DP));
        return drawable;
    }

    RippleDrawable ripple() {
        return new RippleDrawable(ColorStateList.valueOf(this.secondary & RIPPLE_ALPHA_MASK), null, surface());
    }

    private int color(int attribute) {
        TypedValue value = new TypedValue();
        this.context.getTheme().resolveAttribute(attribute, value, true);
        return (value.resourceId != 0) ? this.context.getResources().getColor(value.resourceId) : value.data;
    }

    private static int channel(int value) {
        return Math.max(0, Math.min(MAX_CHANNEL, value));
    }

}

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
package app.hxreborn.extension.outlook.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;

final class SettingsStyle extends app.hxreborn.extension.shared.patchesmenu.SettingsStyle {

    private static final int DIVIDER_ALPHA = 0x1F;

    private static final int CARD_OVERLAY_ALPHA = 0x0F;

    private static final int RIPPLE_ALPHA = 0x33;

    private static final int OPAQUE_ALPHA = 0xFF;

    SettingsStyle(Context context) {
        this(context, isDark(context));
    }

    private SettingsStyle(Context context, boolean dark) {
        this(context, dark, themeColor(context, android.R.attr.colorBackground, dark ? Color.BLACK : Color.WHITE),
                themeColor(context, android.R.attr.textColorPrimary, dark ? Color.WHITE : Color.BLACK));
    }

    private SettingsStyle(Context context, boolean dark, int background, int text) {
        super(context, dark, background, blend(background, text, CARD_OVERLAY_ALPHA), withAlpha(text, DIVIDER_ALPHA),
                text, themeColor(context, android.R.attr.textColorSecondary, text),
                themeColor(context, android.R.attr.colorAccent, text), withAlpha(text, RIPPLE_ALPHA),
                Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    @Override
    protected int backIconResource() {
        return backIconId();
    }

    static int backIconId() {
        return 0;
    }

    static int unreadIconId() {
        return 0;
    }

    static int adsIconId() {
        return 0;
    }

    static int templatesIconId() {
        return 0;
    }

    static int searchIconId() {
        return 0;
    }

    static int copilotIconId() {
        return 0;
    }

    static int shortcutIconId() {
        return 0;
    }

    static int calendarTabIconId() {
        return 0;
    }

    static int appsTabIconId() {
        return 0;
    }

    static int navigationBarIconId() {
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

    private static boolean isDark(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int themeColor(Context context, int attribute, int fallback) {
        TypedArray values = context.obtainStyledAttributes(new int[] { attribute });
        try {
            return values.getColor(0, fallback);
        } finally {
            values.recycle();
        }
    }

    static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int base, int overlay, int alpha) {
        float weight = alpha / (float) OPAQUE_ALPHA;
        int red = Math.round(Color.red(base) * (1 - weight) + Color.red(overlay) * weight);
        int green = Math.round(Color.green(base) * (1 - weight) + Color.green(overlay) * weight);
        int blue = Math.round(Color.blue(base) * (1 - weight) + Color.blue(overlay) * weight);
        return Color.rgb(red, green, blue);
    }

}

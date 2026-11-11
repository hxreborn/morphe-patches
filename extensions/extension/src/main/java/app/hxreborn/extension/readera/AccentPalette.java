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

import java.util.Locale;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

final class AccentPalette {

    static final String STOCK = "stock";

    static final String SYSTEM = "system";

    static final Fixed DEFAULT_FIXED = Fixed.GREEN;

    private static final String PREFERENCE_KEY = "accent_color";

    private static final Pattern HEX_PRESET = Pattern.compile("#[0-9a-fA-F]{6}");

    private AccentPalette() {
    }

    static String getPreset(Context context) {
        String preset;
        try {
            preset = PatchSettings.preferences(context).getString(PREFERENCE_KEY, defaultPreset());
        } catch (ClassCastException ex) {
            Log.w(PatchSettings.LOG_TAG, "Accent selection has an invalid preference type", ex);
            return defaultPreset();
        }
        if (!valid(preset) || (SYSTEM.equals(preset) && !isSystemAvailable())) {
            return defaultPreset();
        }
        return normalized(preset);
    }

    static boolean setPreset(Context context, String preset) {
        if (!valid(preset)) {
            return false;
        }
        return PatchSettings.preferences(context).edit().putString(PREFERENCE_KEY, normalized(preset)).commit();
    }

    static String defaultPreset() {
        return isSystemAvailable() ? SYSTEM : DEFAULT_FIXED.preset;
    }

    static boolean isSystemAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    static boolean isHex(String preset) {
        return HEX_PRESET.matcher(preset).matches();
    }

    private static String normalized(String preset) {
        return isHex(preset) ? preset.toUpperCase(Locale.ROOT) : preset;
    }

    static boolean isCustom(String preset) {
        return isHex(preset) && findFixed(preset) == null;
    }

    static String label(String preset) {
        if (STOCK.equals(preset)) {
            return "ReadEra";
        }
        if (SYSTEM.equals(preset)) {
            return "Material You";
        }
        Fixed fixed = findFixed(preset);
        return (fixed != null) ? fixed.label : "Custom";
    }

    static int color(Context context, String preset) {
        return SYSTEM.equals(preset) ? systemColor(context) : Color.parseColor(preset);
    }

    @TargetApi(Build.VERSION_CODES.S)
    private static int systemColor(Context context) {
        return context.getColor(android.R.color.system_accent1_200);
    }

    private static Fixed findFixed(String preset) {
        for (Fixed fixed : Fixed.values()) {
            if (fixed.preset.equalsIgnoreCase(preset)) {
                return fixed;
            }
        }
        return null;
    }

    private static boolean valid(String preset) {
        return STOCK.equals(preset) || SYSTEM.equals(preset) || isHex(preset);
    }

    enum Fixed {

        RED("Red", 0xFFEF9A9A), ORANGE("Orange", 0xFFFFCC80), GREEN("Green", 0xFFA5D6A7), TEAL("Teal", 0xFF80CBC4),
        CYAN("Cyan", 0xFF80DEEA), BLUE("Blue", 0xFF90CAF9), PURPLE("Purple", 0xFFCE93D8), PINK("Pink", 0xFFF48FB1);

        private static final int RGB_MASK = 0x00FFFFFF;

        final String label;

        final int color;

        final String preset;

        Fixed(String label, int color) {
            this.label = label;
            this.color = color;
            this.preset = String.format(Locale.ROOT, "#%06X", color & RGB_MASK);
        }

    }

}

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

import java.util.Locale;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.Build;
import android.util.Log;

final class AccentPalette {

    static final String STOCK = "";

    static final String SYSTEM = "system";

    private static final String PREFERENCE_KEY = "accent_color";

    private static final int STOCK_LIGHT_PRIMARY = 0xff541597;

    private static final int STOCK_DARK_PRIMARY = 0xffc499ff;

    private static final int STOCK_LIGHT_SURFACE = 0xffeee5f7;

    private static final int STOCK_DARK_SURFACE = 0xff332343;

    private static final int RGB_MASK = 0x00ffffff;

    private static final int ALPHA_MASK = 0xff000000;

    private static final double MIN_CHROMA = 1;

    private static final double MAX_CHROMA_SCALE = 2.5;

    private static final double GAMUT_TOLERANCE = 0.5;

    private static final int GAMUT_STEPS = 12;

    private static final Pattern HEX_PRESET = Pattern.compile("#[0-9a-fA-F]{6}");

    private AccentPalette() {
    }

    static String getPreset(Context context) {
        if (context == null) {
            return STOCK;
        }
        try {
            String preset = preferences(context).getString(PREFERENCE_KEY, STOCK);
            if (!valid(preset)) {
                return STOCK;
            }
            return SYSTEM.equals(preset) ? SYSTEM : preset.toUpperCase(Locale.ROOT);
        } catch (ClassCastException ex) {
            Log.w(PatchSettings.LOG_TAG, "Accent selection has an invalid preference type", ex);
            return STOCK;
        }
    }

    static boolean setPreset(Context context, String preset) {
        if (context == null || !valid(preset)) {
            return false;
        }
        SharedPreferences.Editor editor = preferences(context).edit();
        if (STOCK.equals(preset)) {
            editor.remove(PREFERENCE_KEY);
        } else {
            editor.putString(PREFERENCE_KEY, SYSTEM.equals(preset) ? SYSTEM : preset.toUpperCase(Locale.ROOT));
        }
        return editor.commit();
    }

    static int primary(Context context, boolean dark) {
        return transform(context, dark ? STOCK_DARK_PRIMARY : STOCK_LIGHT_PRIMARY);
    }

    static int surface(Context context, boolean dark) {
        return transform(context, dark ? STOCK_DARK_SURFACE : STOCK_LIGHT_SURFACE);
    }

    static int transform(Context context, int original) {
        String preset = getPreset(context);
        if (STOCK.equals(preset) || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return original;
        }
        int picked;
        if (SYSTEM.equals(preset)) {
            if (!isSystemAvailable()) {
                return original;
            }
            try {
                picked = systemColor(context);
            } catch (Resources.NotFoundException ex) {
                Log.w(PatchSettings.LOG_TAG, "System accent resource could not be read", ex);
                return original;
            }
        } else {
            picked = Color.parseColor(preset);
        }
        return rotate(original, picked);
    }

    static boolean isSystemAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    static String label(Context context) {
        String preset = getPreset(context);
        if (STOCK.equals(preset)) {
            return "ULTIMEA purple";
        }
        if (SYSTEM.equals(preset)) {
            return "Material You";
        }
        for (Fixed fixed : Fixed.values()) {
            if (fixed.preset.equals(preset)) {
                return fixed.label;
            }
        }
        return "Custom " + preset;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static boolean valid(String preset) {
        return preset != null
                && (STOCK.equals(preset) || SYSTEM.equals(preset) || HEX_PRESET.matcher(preset).matches());
    }

    @TargetApi(Build.VERSION_CODES.S)
    private static int systemColor(Context context) {
        return context.getColor(android.R.color.system_accent1_500);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static int rotate(int original, int picked) {
        ColorSpace lab = ColorSpace.get(ColorSpace.Named.CIE_LAB);
        float[] source = Color.valueOf(original).convert(lab).getComponents();
        float[] target = Color.valueOf(picked).convert(lab).getComponents();
        double sourceChroma = Math.hypot(source[1], source[2]);
        double targetChroma = Math.hypot(target[1], target[2]);
        if (sourceChroma < MIN_CHROMA) {
            return original;
        }
        if (targetChroma < MIN_CHROMA) {
            return (original & ALPHA_MASK) | (color(source[0], 0, 0, lab) & RGB_MASK);
        }
        float[] stock = Color.valueOf(STOCK_DARK_PRIMARY).convert(lab).getComponents();
        double stockChroma = Math.hypot(stock[1], stock[2]);
        double scale = Math.min(MAX_CHROMA_SCALE, targetChroma / stockChroma);
        double requested = sourceChroma * (1 + (scale - 1) * Math.min(1, sourceChroma / stockChroma));
        double hue = Math.atan2(target[2], target[1]);
        double chroma = fitChroma(source[0], requested, hue, lab);
        int transformed = color(source[0], chroma, hue, lab);
        return (original & ALPHA_MASK) | (transformed & RGB_MASK);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static double fitChroma(float lightness, double requested, double hue, ColorSpace lab) {
        if (inGamut(lightness, requested, hue, lab)) {
            return requested;
        }
        double low = 0;
        double high = requested;
        for (int step = 0; step < GAMUT_STEPS; step++) {
            double middle = (low + high) / 2;
            if (inGamut(lightness, middle, hue, lab)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static boolean inGamut(float lightness, double chroma, double hue, ColorSpace lab) {
        float[] actual = Color.valueOf(color(lightness, chroma, hue, lab)).convert(lab).getComponents();
        return Math.abs(actual[0] - lightness) <= GAMUT_TOLERANCE
                && Math.hypot(actual[1], actual[2]) >= chroma - GAMUT_TOLERANCE;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static int color(float lightness, double chroma, double hue, ColorSpace lab) {
        return Color.valueOf(lightness, (float) (chroma * Math.cos(hue)), (float) (chroma * Math.sin(hue)), 1, lab)
            .convert(ColorSpace.get(ColorSpace.Named.SRGB))
            .toArgb();
    }

    enum Fixed {

        RED("Red", 0), ORANGE("Orange", 28), GOLD("Gold", 48), GREEN("Green", 105), TEAL("Teal", 168),
        CYAN("Cyan", 195), BLUE("Blue", 220), PINK("Pink", 325);

        final String label;

        final String preset;

        Fixed(String label, int hue) {
            this.label = label;
            this.preset = String.format(Locale.ROOT, "#%06X", Color.HSVToColor(new float[] { hue, 1, 1 }) & RGB_MASK);
        }

    }

}

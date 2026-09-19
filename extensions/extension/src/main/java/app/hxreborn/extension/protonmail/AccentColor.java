/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.Build;
import android.webkit.WebView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.hxreborn.extension.WebAssets;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class AccentColor {
    private static final String PREFERENCE_KEY = "accent_color";

    static final String STOCK = "";
    static final String SYSTEM = "system";

    static final int STOCK_LIGHT_ACCENT = 0xFF6D4AFF;
    static final int STOCK_DARK_ACCENT = 0xFF9292F9;

    private static final long ARGB_MASK = 0xFFFFFFFFL;
    private static final double MAX_CHROMA_SCALE = 2.5;
    private static final double MIN_PRESET_CHROMA = 1;
    private static final double MIN_LIGHTNESS_SCALE = 0.6;
    private static final double MAX_LIGHTNESS_SCALE = 1.4;
    private static final float MAX_LIGHTNESS = 100;
    private static final double GAMUT_TOLERANCE = 2;
    private static final int GAMUT_STEPS = 10;

    private static final Map<Integer, Integer> RECOLORED_COLORS = new ConcurrentHashMap<>();

    private AccentColor() {}

    public static boolean isPatched() {
        return false;
    }

    static boolean isSystemAccentAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    static String getPreset() {
        return isPatched() ? PatchSettings.getString(PREFERENCE_KEY, STOCK) : STOCK;
    }

    static void setPreset(String preset) {
        PatchSettings.setString(PREFERENCE_KEY, preset);
        RECOLORED_COLORS.clear();
    }

    static int getAccentColor(boolean dark) {
        return resolveAccentColor(getPreset(), dark);
    }

    static int resolveAccentColor(String preset, boolean dark) {
        final int stockAccent = dark ? STOCK_DARK_ACCENT : STOCK_LIGHT_ACCENT;
        final LabAdjustment adjustment = computeLabAdjustment(preset);
        return adjustment == null ? stockAccent : applyLabAdjustment(stockAccent, adjustment);
    }

    static int resolvePresetColor(String preset) {
        return SYSTEM.equals(preset) ? getSystemAccentColor() : parseColorOrZero(preset);
    }

    public static long applyAccentToArgb(long original) {
        try {
            final LabAdjustment adjustment = computeLabAdjustment(getPreset());
            if (adjustment == null) return original;

            final int source = (int) original;
            Integer accentColor = RECOLORED_COLORS.get(source);
            if (accentColor == null) {
                accentColor = applyLabAdjustment(source, adjustment);
                RECOLORED_COLORS.put(source, accentColor);
            }

            return accentColor & ARGB_MASK;
        } catch (Throwable t) {
            return original;
        }
    }

    static void injectSettingsWebViewStyle(WebView view) {
        try {
            if (view == null) return;

            final LabAdjustment adjustment = computeLabAdjustment(getPreset());
            if (adjustment == null) return;

            final float[] stockHsl = argbToHsl(STOCK_DARK_ACCENT);
            final float[] accentHsl = argbToHsl(applyLabAdjustment(STOCK_DARK_ACCENT, adjustment));
            final float saturationScale = stockHsl[1] == 0 ? 1 : accentHsl[1] / stockHsl[1];

            view.evaluateJavascript(
                    WebAssets.ACCENT_RECOLOR
                            .replace("__TONES__", serializeColorMap())
                            .replace("__SHIFT__", Float.toString(accentHsl[0] - stockHsl[0]))
                            .replace("__SATURATION__", Float.toString(saturationScale)),
                    null);
        } catch (Throwable t) {
            Logger.printException(() -> "Could not style the settings web view", t);
        }
    }

    static boolean isSettingsWebViewStyleEnabled() {
        return computeLabAdjustment(getPreset()) != null;
    }

    private static float[] argbToHsl(int argb) {
        final float red = Color.red(argb) / 255f;
        final float green = Color.green(argb) / 255f;
        final float blue = Color.blue(argb) / 255f;
        final float max = Math.max(red, Math.max(green, blue));
        final float min = Math.min(red, Math.min(green, blue));
        final float delta = max - min;
        final float lightness = (max + min) / 2f;
        if (delta == 0) return new float[] {0, 0, lightness};

        final float saturation = lightness > 0.5f
                ? delta / (2f - max - min)
                : delta / (max + min);
        final float hue;
        if (max == red) {
            hue = (green - blue) / delta + (green < blue ? 6 : 0);
        } else if (max == green) {
            hue = (blue - red) / delta + 2;
        } else {
            hue = (red - green) / delta + 4;
        }

        return new float[] {hue * 60f, saturation, lightness};
    }

    private static String serializeColorMap() {
        final StringBuilder map = new StringBuilder("{");
        for (Map.Entry<Integer, Integer> entry : RECOLORED_COLORS.entrySet()) {
            if (map.length() > 1) map.append(',');
            map.append('\'').append(rgbChannelString(entry.getKey())).append("':'")
                    .append(rgbChannelString(entry.getValue())).append('\'');
        }

        return map.append('}').toString();
    }

    private static String rgbChannelString(int argb) {
        return Color.red(argb) + "," + Color.green(argb) + "," + Color.blue(argb);
    }

    private static LabAdjustment computeLabAdjustment(String preset) {
        if (preset == null || preset.isEmpty()) return null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null;

        final int picked = resolvePresetColor(preset);
        if (picked == 0) return null;

        final float[] pickedLab = argbToLab(picked);
        final float[] stockLab = argbToLab(STOCK_DARK_ACCENT);
        final double pickedChroma = Math.hypot(pickedLab[1], pickedLab[2]);
        if (pickedChroma < MIN_PRESET_CHROMA) return null;

        final double stockChroma = Math.hypot(stockLab[1], stockLab[2]);
        return new LabAdjustment(
                Math.atan2(pickedLab[2], pickedLab[1]),
                Math.min(MAX_CHROMA_SCALE, pickedChroma / stockChroma),
                stockLab[0] == 0 ? 1 : Math.max(MIN_LIGHTNESS_SCALE,
                        Math.min(MAX_LIGHTNESS_SCALE, pickedLab[0] / stockLab[0])),
                stockChroma);
    }

    @TargetApi(Build.VERSION_CODES.S)
    private static int getSystemAccentColor() {
        if (!isSystemAccentAvailable()) return 0;

        final Context context = Utils.getContext();
        return context == null ? 0 : context.getColor(android.R.color.system_accent1_500);
    }

    private static int parseColorOrZero(String color) {
        try {
            return Color.parseColor(color);
        } catch (Throwable t) {
            return 0;
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static int applyLabAdjustment(int argb, LabAdjustment adjustment) {
        final float[] labComponents = argbToLab(argb);
        final float lightness = (float) Math.max(0,
                Math.min(MAX_LIGHTNESS, labComponents[0] * adjustment.lightnessScale));
        final double sourceChroma = Math.hypot(labComponents[1], labComponents[2]);
        final double boost = 1 + (adjustment.chromaScale - 1)
                * Math.min(1, sourceChroma / adjustment.stockChroma);
        final double requested = sourceChroma * boost;
        final double chroma =
                fitChromaToGamut(lightness, requested, adjustment.hueRadians, labComponents[3]);

        return labToArgb(lightness, chroma, adjustment.hueRadians, labComponents[3]);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static double fitChromaToGamut(float lightness, double chroma, double hue, float alpha) {
        if (isInGamut(lightness, chroma, hue, alpha)) return chroma;

        double low = 0;
        double high = chroma;
        for (int step = 0; step < GAMUT_STEPS; step++) {
            final double middle = (low + high) / 2;
            if (isInGamut(lightness, middle, hue, alpha)) {
                low = middle;
            } else {
                high = middle;
            }
        }

        return low;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static boolean isInGamut(float lightness, double chroma, double hue, float alpha) {
        final float[] roundTrip = argbToLab(labToArgb(lightness, chroma, hue, alpha));
        return Math.abs(roundTrip[0] - lightness) <= GAMUT_TOLERANCE
                && Math.hypot(roundTrip[1], roundTrip[2]) >= chroma - GAMUT_TOLERANCE;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static int labToArgb(float lightness, double chroma, double hue, float alpha) {
        return Color.valueOf(lightness, (float) (chroma * Math.cos(hue)),
                        (float) (chroma * Math.sin(hue)), alpha, getLabColorSpace())
                .convert(ColorSpace.get(ColorSpace.Named.SRGB))
                .toArgb();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static float[] argbToLab(int argb) {
        return Color.valueOf(argb).convert(getLabColorSpace()).getComponents();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static ColorSpace getLabColorSpace() {
        return ColorSpace.get(ColorSpace.Named.CIE_LAB);
    }

    private static final class LabAdjustment {
        final double hueRadians;
        final double chromaScale;
        final double lightnessScale;
        final double stockChroma;

        LabAdjustment(double hueRadians, double chromaScale, double lightnessScale,
                double stockChroma) {
            this.hueRadians = hueRadians;
            this.chromaScale = chromaScale;
            this.lightnessScale = lightnessScale;
            this.stockChroma = stockChroma;
        }
    }
}

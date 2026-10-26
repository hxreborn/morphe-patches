/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.O)
public final class AccentTransform {

    private static final double MAX_CHROMA_SCALE = 2.5;

    private static final double MIN_PICKED_CHROMA = 1;

    private static final float MAX_LIGHTNESS = 100;

    private static final double GAMUT_TOLERANCE = 2;

    private static final int GAMUT_STEPS = 10;

    private final double hueRadians;

    private final double chromaScale;

    private final float stockLightness;

    private final float lightnessShift;

    private final double stockChroma;

    private AccentTransform(double hueRadians, double chromaScale, float stockLightness, float lightnessShift,
            double stockChroma) {
        this.hueRadians = hueRadians;
        this.chromaScale = chromaScale;
        this.stockLightness = stockLightness;
        this.lightnessShift = lightnessShift;
        this.stockChroma = stockChroma;
    }

    public static AccentTransform between(int stockAccent, int pickedAccent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }

        final float[] pickedLab = argbToLab(pickedAccent);
        final double pickedChroma = Math.hypot(pickedLab[1], pickedLab[2]);
        if (pickedChroma < MIN_PICKED_CHROMA) {
            return null;
        }

        final float[] stockLab = argbToLab(stockAccent);
        final double stockChroma = Math.hypot(stockLab[1], stockLab[2]);
        return new AccentTransform(Math.atan2(pickedLab[2], pickedLab[1]),
                Math.min(MAX_CHROMA_SCALE, pickedChroma / stockChroma), stockLab[0], pickedLab[0] - stockLab[0],
                stockChroma);
    }

    public int apply(int argb) {
        final float[] lab = argbToLab(argb);
        final float lightness = shiftLightness(lab[0]);
        final double sourceChroma = Math.hypot(lab[1], lab[2]);
        final double boost = 1 + (this.chromaScale - 1) * Math.min(1, sourceChroma / this.stockChroma);
        final double chroma = fitChromaToGamut(lightness, sourceChroma * boost, lab[3]);
        return labToArgb(lightness, chroma, this.hueRadians, lab[3]);
    }

    private float shiftLightness(float lightness) {
        final float clamped = Math.max(0, Math.min(MAX_LIGHTNESS, lightness));
        final float weight = (clamped <= this.stockLightness) ? clamped / this.stockLightness
                : (MAX_LIGHTNESS - clamped) / (MAX_LIGHTNESS - this.stockLightness);
        return clamped + this.lightnessShift * weight;
    }

    private double fitChromaToGamut(float lightness, double chroma, float alpha) {
        if (isInGamut(lightness, chroma, alpha)) {
            return chroma;
        }

        double low = 0;
        double high = chroma;
        for (int step = 0; step < GAMUT_STEPS; step++) {
            final double middle = (low + high) / 2;
            if (isInGamut(lightness, middle, alpha)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private boolean isInGamut(float lightness, double chroma, float alpha) {
        final float[] roundTrip = argbToLab(labToArgb(lightness, chroma, this.hueRadians, alpha));
        return Math.abs(roundTrip[0] - lightness) <= GAMUT_TOLERANCE
                && Math.hypot(roundTrip[1], roundTrip[2]) >= chroma - GAMUT_TOLERANCE;
    }

    private static int labToArgb(float lightness, double chroma, double hue, float alpha) {
        return Color
            .valueOf(lightness, (float) (chroma * Math.cos(hue)), (float) (chroma * Math.sin(hue)), alpha,
                    labColorSpace())
            .convert(ColorSpace.get(ColorSpace.Named.SRGB))
            .toArgb();
    }

    private static float[] argbToLab(int argb) {
        return Color.valueOf(argb).convert(labColorSpace()).getComponents();
    }

    private static ColorSpace labColorSpace() {
        return ColorSpace.get(ColorSpace.Named.CIE_LAB);
    }

}

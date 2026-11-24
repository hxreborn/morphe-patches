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
package app.hxreborn.extension.outlook.theme;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.O)
final class ThemeColorRamp {

    private static final float MAX_LIGHTNESS = 100;

    private static final double MIN_CONTRAST_WITH_WHITE = 3;

    private static final double CONTRAST_FLARE = 0.05;

    private static final double LIGHTNESS_SCALE = 116;

    private static final double LIGHTNESS_OFFSET = 16;

    private static final float MAX_WHITE_TEXT_LIGHTNESS = (float) (LIGHTNESS_SCALE
            * Math.cbrt((1 + CONTRAST_FLARE) / MIN_CONTRAST_WITH_WHITE - CONTRAST_FLARE) - LIGHTNESS_OFFSET);

    private static final double GAMUT_TOLERANCE = 1;

    private static final int GAMUT_STEPS = 12;

    private static final int LIGHTNESS = 0;

    private static final int GREEN_RED = 1;

    private static final int BLUE_YELLOW = 2;

    private static final int ALPHA = 3;

    private final float referenceLightness;

    private final float pickedLightness;

    private final double chromaScale;

    private final double hueRadians;

    private ThemeColorRamp(float referenceLightness, float pickedLightness, double chromaScale, double hueRadians) {
        this.referenceLightness = referenceLightness;
        this.pickedLightness = pickedLightness;
        this.chromaScale = chromaScale;
        this.hueRadians = hueRadians;
    }

    static ThemeColorRamp between(int referenceArgb, int pickedArgb) {
        float[] reference = toLab(referenceArgb);
        float[] picked = toLab(pickedArgb);
        double referenceChroma = chroma(reference);
        double chromaScale = (referenceChroma == 0) ? 0 : chroma(picked) / referenceChroma;
        return new ThemeColorRamp(reference[LIGHTNESS], Math.min(picked[LIGHTNESS], MAX_WHITE_TEXT_LIGHTNESS),
                chromaScale, Math.atan2(picked[BLUE_YELLOW], picked[GREEN_RED]));
    }

    int transform(int argb) {
        float[] lab = toLab(argb);
        float lightness = mapLightness(lab[LIGHTNESS]);
        double chroma = fitChromaToGamut(lightness, chroma(lab) * this.chromaScale, lab[ALPHA]);
        return toArgb(lightness, chroma, lab[ALPHA]);
    }

    static boolean hasWhiteTextContrast(int argb) {
        return toLab(argb)[LIGHTNESS] <= MAX_WHITE_TEXT_LIGHTNESS;
    }

    static double distance(int firstArgb, int secondArgb) {
        float[] first = toLab(firstArgb);
        float[] second = toLab(secondArgb);
        return Math.sqrt(square(first[LIGHTNESS] - second[LIGHTNESS]) + square(first[GREEN_RED] - second[GREEN_RED])
                + square(first[BLUE_YELLOW] - second[BLUE_YELLOW]));
    }

    private float mapLightness(float lightness) {
        if (lightness <= this.referenceLightness) {
            return (this.referenceLightness == 0) ? this.pickedLightness
                    : lightness * this.pickedLightness / this.referenceLightness;
        }
        return this.pickedLightness + (lightness - this.referenceLightness) * (MAX_LIGHTNESS - this.pickedLightness)
                / (MAX_LIGHTNESS - this.referenceLightness);
    }

    private double fitChromaToGamut(float lightness, double chroma, float alpha) {
        if (isInGamut(lightness, chroma, alpha)) {
            return chroma;
        }
        double low = 0;
        double high = chroma;
        for (int step = 0; step < GAMUT_STEPS; step++) {
            double middle = (low + high) / 2;
            if (isInGamut(lightness, middle, alpha)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private boolean isInGamut(float lightness, double chroma, float alpha) {
        float[] roundTrip = toLab(toArgb(lightness, chroma, alpha));
        return Math.abs(roundTrip[LIGHTNESS] - lightness) <= GAMUT_TOLERANCE
                && chroma(roundTrip) >= chroma - GAMUT_TOLERANCE;
    }

    private int toArgb(float lightness, double chroma, float alpha) {
        return Color
            .valueOf(lightness, (float) (chroma * Math.cos(this.hueRadians)),
                    (float) (chroma * Math.sin(this.hueRadians)), alpha, ColorSpace.get(ColorSpace.Named.CIE_LAB))
            .convert(ColorSpace.get(ColorSpace.Named.SRGB))
            .toArgb();
    }

    private static double square(double value) {
        return value * value;
    }

    private static double chroma(float[] lab) {
        return Math.hypot(lab[GREEN_RED], lab[BLUE_YELLOW]);
    }

    private static float[] toLab(int argb) {
        return Color.valueOf(argb).convert(ColorSpace.get(ColorSpace.Named.CIE_LAB)).getComponents();
    }

}

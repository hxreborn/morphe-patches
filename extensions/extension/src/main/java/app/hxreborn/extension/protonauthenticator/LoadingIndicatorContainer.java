/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonauthenticator;

import android.graphics.Color;

import app.hxreborn.extension.proton.LoadingIndicator;

@SuppressWarnings("unused")
public final class LoadingIndicatorContainer {

    private static final float PRIMARY_MAX_CHROMA = 36f;

    private static final float LIGHT_PRIMARY_CONTAINER_TONE = 90f;

    private static final float DARK_PRIMARY_CONTAINER_TONE = 30f;

    private static final float RED_LUMA = 0.299f;

    private static final float GREEN_LUMA = 0.587f;

    private static final float BLUE_LUMA = 0.114f;

    private static final float LIGHT_SURFACE_MIN_LUMA = 127.5f;

    private static final long ARGB_MASK = 0xFFFFFFFFL;

    private LoadingIndicatorContainer() {

    }

    public static long containerColor(long surfaceColor, long accentColor) {
        if (!LoadingIndicator.isEnabled()) {
            return surfaceColor;
        }

        final float tone = (isLight(argb(surfaceColor))) ? LIGHT_PRIMARY_CONTAINER_TONE : DARK_PRIMARY_CONTAINER_TONE;
        final int container = toneWithMaxChroma(argb(accentColor), PRIMARY_MAX_CHROMA, tone);
        return (container & ARGB_MASK) << 32;
    }

    static int toneWithMaxChroma(int argb, float maxChroma, float tone) {
        return argb;
    }

    private static boolean isLight(int argb) {
        return RED_LUMA * Color.red(argb) + GREEN_LUMA * Color.green(argb)
                + BLUE_LUMA * Color.blue(argb) >= LIGHT_SURFACE_MIN_LUMA;
    }

    private static int argb(long packedColor) {
        return (int) (packedColor >>> 32);
    }

}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.graphics.Color;
import android.view.View;
import android.webkit.WebView;

import java.util.Map;

import app.hxreborn.extension.WebAssets;
import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.AmoledTheme;
import app.morphe.extension.shared.Logger;

@SuppressWarnings("unused")
public final class WebSettingsTheme {
    private static final int STYLE_CHECK_INTERVAL_MS = 100;
    private static final int WEBVIEW_VISIBILITY_TIMEOUT_MS = 2500;

    private WebSettingsTheme() {}

    public static void hideBeforeStyling(WebView view) {
        try {
            if (view == null || !hasEnabledStyle()) return;

            view.setVisibility(View.INVISIBLE);
            view.postDelayed(() -> view.setVisibility(View.VISIBLE), WEBVIEW_VISIBILITY_TIMEOUT_MS);
        } catch (Throwable t) {
            Logger.printException(() -> "Could not hide the settings web view before styling", t);
        }
    }

    public static void injectEnabledStyles(WebView view) {
        try {
            if (view == null) return;

            injectAmoledStyle(view);
            injectAccentStyle(view);
            if (AccentColor.hasCustomAccent()) {
                showWhenAccentStyled(view);
            } else {
                view.setVisibility(View.VISIBLE);
            }
        } catch (Throwable t) {
            Logger.printException(() -> "Could not style the settings web view", t);
        }
    }

    private static boolean hasEnabledStyle() {
        return AmoledTheme.isEnabled() || AccentColor.hasCustomAccent();
    }

    private static void injectAmoledStyle(WebView view) {
        if (!AmoledTheme.isEnabled()) return;
        view.evaluateJavascript(WebAssets.AMOLED_WEBVIEW, null);
    }

    private static void injectAccentStyle(WebView view) {
        try {
            if (!AccentColor.hasCustomAccent()) return;

            final float[] stockHsl = argbToHsl(AccentColor.STOCK_DARK_ACCENT);
            final float[] accentHsl = argbToHsl(AccentColor.transformedStockDarkAccent());
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
        for (Map.Entry<Integer, Integer> entry : AccentColor.transformedBrandColors().entrySet()) {
            if (map.length() > 1) map.append(',');
            map.append('\'').append(rgbChannelString(entry.getKey())).append("':'")
                    .append(rgbChannelString(entry.getValue())).append('\'');
        }

        return map.append('}').toString();
    }

    private static String rgbChannelString(int argb) {
        return Color.red(argb) + "," + Color.green(argb) + "," + Color.blue(argb);
    }

    private static void showWhenAccentStyled(WebView view) {
        if (view.getVisibility() == View.VISIBLE) return;

        view.evaluateJavascript(WebAssets.ACCENT_STYLE_READY, ready -> {
            if (Boolean.parseBoolean(ready)) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.postDelayed(() -> showWhenAccentStyled(view), STYLE_CHECK_INTERVAL_MS);
            }
        });
    }
}

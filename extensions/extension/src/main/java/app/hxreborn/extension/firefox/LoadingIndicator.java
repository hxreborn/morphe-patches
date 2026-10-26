/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hxreborn/pixel-nits:
 * app/src/main/kotlin/eu/hxreborn/pixelinjector/xposed/hook/firefox/FirefoxLoadingIndicator.kt
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Field;

import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import app.hxreborn.extension.shared.LoadingIndicatorDrawable;
import app.hxreborn.extension.shared.NightMode;

@SuppressWarnings("unused")
public final class LoadingIndicator {

    private static final String TAG = "FirefoxPatches";

    private static final float MAX_PROGRESS_TRIM = 0.8f;

    private static final float CIRCLE_DIAMETER_DP = 48f;

    private LoadingIndicator() {

    }

    public static float circleDiameter(float stock) {
        return PatchSettings.loadingIndicator() ? CIRCLE_DIAMETER_DP : stock;
    }

    public static void install(ViewGroup layout) {
        if (!PatchSettings.loadingIndicator()) {
            return;
        }
        try {
            ImageView circle = null;
            for (int i = 0; i < layout.getChildCount() && circle == null; i++) {
                if (layout.getChildAt(i) instanceof ImageView) {
                    circle = (ImageView) layout.getChildAt(i);
                }
            }
            final Drawable stock = (circle != null) ? circle.getDrawable() : null;
            if (!(stock instanceof Animatable)) {
                return;
            }
            final Object ring = declaredField(stock.getClass(), "mRing").get(stock);
            final Field endTrim = declaredField(ring.getClass(), "mEndTrim");
            boolean night = NightMode.isActive(layout.getContext());
            int accent = AccentColor.themeAccent(night);
            int surface = night ? 0xFF2B2A33 : 0xFFFFFFFF;
            int container = blend(surface, accent, night ? 0.32f : 0.24f);
            LoadingIndicatorDrawable indicator = new LoadingIndicatorDrawable(stock, container, accent, () -> {
                try {
                    return endTrim.getFloat(ring) / MAX_PROGRESS_TRIM;
                } catch (IllegalAccessException ex) {
                    return 0f;
                }
            });
            circle.setBackground(new ColorDrawable(Color.TRANSPARENT));
            circle.setOutlineProvider(new OvalOutline());
            circle.setImageDrawable(indicator);
            stock.setCallback(indicator);
        } catch (Throwable ex) {
            Log.e(TAG, "loading indicator failed", ex);
        }
    }

    private static Field declaredField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static int blend(int from, int to, float amount) {
        int red = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * amount);
        int green = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * amount);
        int blue = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount);
        return Color.rgb(red, green, blue);
    }

    private static final class OvalOutline extends ViewOutlineProvider {

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }

    }

}

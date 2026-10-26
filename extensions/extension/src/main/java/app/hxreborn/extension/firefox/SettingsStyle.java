/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;

final class SettingsStyle {

    private static final String TAG = "FirefoxPatches";

    private static final String ROLE_PREFIX = "fx_mobile_";

    private static final String OLED_ROLE_PREFIX = "fx_mobile_oled_";

    final boolean night;

    final int sheet;

    final int card;

    final int bar;

    final int barHigh;

    final int text;

    final int weak;

    final int accent;

    final int onAccent;

    final int defaultAccent;

    final int accentContainer;

    final int onAccentContainer;

    final int selected;

    final int onSelected;

    final int outline;

    final int outlineSoft;

    final int error;

    final int errorContainer;

    final int onErrorContainer;

    final int successContainer;

    private final Resources roleResources;

    private final String[] rolePrefixes;

    private SettingsStyle(Activity activity, boolean night) {
        this.night = night;
        boolean amoledPatched = night && PatchSettings.amoledThemeApplied();
        boolean black = amoledPatched && PatchSettings.amoledTheme();
        this.roleResources = roleResources(activity, amoledPatched, black);
        this.rolePrefixes = rolePrefixes(black);
        int container = role(activity, night, "surface_container", 0xFFF7F6FB, 0xFF1D1B1F);
        this.sheet = black ? Color.BLACK : container;
        this.card = black ? container : role(activity, night, "surface_bright", 0xFFFFFFFF, 0xFF312F33);
        this.bar = role(activity, night, "surface_container_highest", 0xFFE3E2E7, 0xFF312F33);
        this.barHigh = role(activity, night, "surface_container_high", 0xFFEFEDF2, 0xFF252428);
        this.text = role(activity, night, "on_surface", 0xFF180E30, 0xFFF2F0F8);
        this.weak = role(activity, night, "on_surface_variant", 0xB3180E30, 0xB3F2F0F8);
        this.accent = resolveAccent(activity, night);
        int primary = role(activity, night, "primary", 0xFF3E2976, 0xFFCDB7FF);
        this.defaultAccent = primary;
        boolean themed = this.accent == primary;
        this.onAccent = themed ? role(activity, night, "on_primary", 0xFFFFFFFF, 0xFF171519)
                : (isLight(this.accent) ? 0xFF15141A : 0xFFFFFFFF);
        this.accentContainer = themed ? role(activity, night, "primary_container", 0xFFCDB7FF, 0xFF5939A8)
                : mix(this.accent, this.sheet, night ? 0.35f : 0.24f);
        this.onAccentContainer = themed ? role(activity, night, "on_primary_container", 0xFF180E30, 0xFFF2F0F8)
                : this.text;
        this.selected = themed ? role(activity, night, "secondary_container", 0xFFE2DCF2, 0xFF3E315F)
                : mix(this.accent, this.sheet, night ? 0.22f : 0.14f);
        this.onSelected = themed ? role(activity, night, "on_secondary_container", 0xFF180E30, 0xFFF2F0F8) : this.text;
        this.outline = role(activity, night, "outline", 0xFF817F84, 0xFF817F84);
        this.outlineSoft = role(activity, night, "outline_variant", 0xFFE3E2E7, 0xFF3F3E42);
        this.error = role(activity, night, "error", 0xFFC52D4F, 0xFFFF8090);
        this.errorContainer = role(activity, night, "error_container", 0xFFFFD0D7, 0xFF69172D);
        this.onErrorContainer = role(activity, night, "on_error_container", 0xFF180E30, 0xFFF2F0F8);
        this.successContainer = role(activity, night, "success_container", 0xFFB8EED9, 0xFF004933);
    }

    static SettingsStyle of(Activity activity, boolean night) {
        return new SettingsStyle(activity, night);
    }

    static int fade(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha), Color.red(color), Color.green(color),
                Color.blue(color));
    }

    static int mix(int top, int bottom, float amount) {
        float rest = 1f - amount;
        return Color.argb(255, Math.round(Color.red(top) * amount + Color.red(bottom) * rest),
                Math.round(Color.green(top) * amount + Color.green(bottom) * rest),
                Math.round(Color.blue(top) * amount + Color.blue(bottom) * rest));
    }

    private static boolean isLight(int color) {
        return (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000 >= 150;
    }

    private static Resources roleResources(Activity activity, boolean amoledPatched, boolean black) {
        if (!amoledPatched || black) {
            return activity.getResources();
        }
        Configuration stock = new Configuration();
        stock.uiMode = Configuration.UI_MODE_TYPE_NORMAL | Configuration.UI_MODE_NIGHT_YES;
        return activity.createConfigurationContext(stock).getResources();
    }

    private static String[] rolePrefixes(boolean black) {
        return black ? new String[] { OLED_ROLE_PREFIX, ROLE_PREFIX } : new String[] { ROLE_PREFIX };
    }

    private int role(Activity activity, boolean night, String role, int light, int dark) {
        try {
            for (String prefix : this.rolePrefixes) {
                int id = this.roleResources.getIdentifier(prefix + role, "color", activity.getPackageName());
                if (id != 0) {
                    return this.roleResources.getColor(id, activity.getTheme());
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, "color " + role + " failed", ex);
        }
        return night ? dark : light;
    }

    private static int resolveAccent(Activity activity, boolean night) {
        if (AccentColor.hasCustomAccent()) {
            return AccentColor.themeAccent(night);
        }
        TypedValue value = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.colorAccent, value, true)
                && value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT
                && Color.alpha(value.data) == 255) {
            return value.data;
        }
        return AccentColor.themeAccent(night);
    }

}

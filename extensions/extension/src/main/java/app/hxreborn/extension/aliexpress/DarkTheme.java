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

package app.hxreborn.extension.aliexpress;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class DarkTheme {

    static final String NATIVE_PREFERENCES_NAME = "ae_enable_startup_optimize_sp";

    static final String NATIVE_CHOICE_MADE_KEY = "is_local_dark_mode_changed";

    static final String NATIVE_CHOICE_KEY = "is_local_dark_mode_open";

    private static final String HOST_WHITE_BACKGROUND = "hx_dark_background_ffffffff";

    private static final String NATIVE_TRUE = "true";

    private static final String NATIVE_FALSE = "false";

    private static final int PAGE_LOADED_MESSAGE = 401;

    private static final long PAGE_COVER_TIMEOUT_MS = 12000;

    private static final long PAGE_SETTLE_MS = 2500;

    private static final int BAR_MIN_ASPECT = 4;

    private static final String PAGE_STYLE_SCRIPT = "var style = document.createElement('style');"
            + "style.textContent = '.header-navbar:not(.header-navbar-fixed)"
            + "{background-color:#2B2B2B;background-blend-mode:multiply}';"
            + "document.documentElement.appendChild(style);";

    private static volatile boolean hostDarkMode;

    private DarkTheme() {
    }

    static int pageStatusId() {
        return 0;
    }

    public static void recordHostDarkMode(boolean enabled) {
        hostDarkMode = enabled;
    }

    public static boolean enableDarkMode(boolean enabled) {
        recordHostDarkMode(enabled);
        return enabled || PatchSettings.darkTheme();
    }

    public static boolean followsDarkMode() {
        return PatchSettings.darkTheme() && nativeSwitchOn();
    }

    public static boolean nativeSwitchOn() {
        Context context = PatchSettings.applicationContext();
        return context != null && nativeSwitchAllowsDark(context);
    }

    public static int templateColor(Object node, int type, int color) {
        if (!templatesNeedNightColors()) {
            return color;
        }
        if (TemplateNodes.keepsLightColors(node)) {
            return color;
        }
        if (type == DarkColors.TEXT_TYPE && !TemplateNodes.drawnOnNightSurface(node)) {
            return color;
        }
        return DarkColors.night(type, color);
    }

    public static void dimBackdropImage(Object node, View image) {
        dimBar(image, TemplateNodes.isBackdropImage(node));
    }

    public static void dimSearchBackdropImage(Object node, View image) {
        dimBar(image, TemplateNodes.hasLightSurface(node));
    }

    private static void dimBar(View image, boolean onLightSurface) {
        if (templatesNeedNightColors() && onLightSurface && image instanceof ImageView
                && isBar(image.getLayoutParams())) {
            image.post(() -> {
                ((ImageView) image).setColorFilter(DarkColors.fillSurface(), PorterDuff.Mode.MULTIPLY);
                if (image.getParent() instanceof ViewGroup) {
                    lightenText((ViewGroup) image.getParent());
                }
            });
        }
    }

    private static void lightenText(ViewGroup group) {
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(DarkColors.primaryText());
            } else if (child instanceof ViewGroup) {
                lightenText((ViewGroup) child);
            }
        }
    }

    private static boolean isBar(ViewGroup.LayoutParams params) {
        return params != null && params.height > 0 && params.width >= params.height * BAR_MIN_ASPECT;
    }

    public static void darkenQuantityControl(LinearLayout control) {
        if (!templatesNeedNightColors()) {
            return;
        }
        int glyph = DarkColors.primaryText();
        for (int index = 0; index < control.getChildCount(); index++) {
            View child = control.getChildAt(index);
            if (child instanceof Button) {
                Drawable background = child.getBackground();
                if (background != null) {
                    background.setColorFilter(glyph, PorterDuff.Mode.SRC_ATOP);
                }
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(glyph);
            }
        }
    }

    public static void coverWebPage(View page) {
        if (!darkScreensActive()) {
            return;
        }
        View status = pageStatus(page);
        if (status == null) {
            page.post(() -> cover(pageStatus(page)));
        } else {
            cover(status);
        }
    }

    private static void cover(View status) {
        if (status != null) {
            status.setBackgroundColor(DarkColors.pageSurface());
            uncover(status, PAGE_COVER_TIMEOUT_MS);
        }
    }

    public static void revealWebPage(View page, Message message) {
        if (message.what != PAGE_LOADED_MESSAGE || !darkScreensActive()) {
            return;
        }
        darkenPageHeader(page);
        View status = pageStatus(page);
        if (status != null) {
            uncover(status, PAGE_SETTLE_MS);
        }
    }

    private static void darkenPageHeader(View page) {
        try {
            page.getClass()
                .getMethod("evaluateJavascript", String.class, ValueCallback.class)
                .invoke(page, PAGE_STYLE_SCRIPT, null);
        } catch (ReflectiveOperationException ex) {
            Log.w(PatchSettings.LOG_TAG, "Page style injection failed", ex);
        }
    }

    private static void uncover(View status, long delayMs) {
        status.postDelayed(() -> status.setBackgroundColor(Color.TRANSPARENT), delayMs);
    }

    private static View pageStatus(View page) {
        return page.getRootView().findViewById(pageStatusId());
    }

    private static boolean templatesNeedNightColors() {
        return !hostDarkMode && darkScreensActive();
    }

    private static boolean darkScreensActive() {
        Context context = PatchSettings.applicationContext();
        return context != null && PatchSettings.darkTheme() && nightActive(context);
    }

    public static void fillNavigationBar(View filler, int color) {
        filler.setBackgroundColor(hostWhiteBackground(filler.getContext(), color));
    }

    static int hostWhiteBackground(Context context, int color) {
        int id = context.getResources().getIdentifier(HOST_WHITE_BACKGROUND, "color", context.getPackageName());
        if (id == 0) {
            return color;
        }
        return context.getResources().getColor(id, context.getTheme());
    }

    static boolean nightActive(Context context) {
        boolean systemNight = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return systemNight && nativeSwitchAllowsDark(context);
    }

    static boolean nativeSwitchAllowsDark(Context context) {
        SharedPreferences nativePreferences = context.getSharedPreferences(NATIVE_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        boolean choiceMade = NATIVE_TRUE.equals(nativePreferences.getString(NATIVE_CHOICE_MADE_KEY, NATIVE_FALSE));
        return !choiceMade || NATIVE_TRUE.equals(nativePreferences.getString(NATIVE_CHOICE_KEY, NATIVE_FALSE));
    }

}

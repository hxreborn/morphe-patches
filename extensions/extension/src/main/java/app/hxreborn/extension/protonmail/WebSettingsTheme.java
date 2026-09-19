/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.view.View;
import android.webkit.WebView;

import app.hxreborn.extension.WebAssets;
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

            AmoledTheme.injectSettingsWebViewStyle(view);
            AccentColor.injectSettingsWebViewStyle(view);
            if (AccentColor.isSettingsWebViewStyleEnabled()) {
                showWhenAccentStyled(view);
            } else {
                view.setVisibility(View.VISIBLE);
            }
        } catch (Throwable t) {
            Logger.printException(() -> "Could not style the settings web view", t);
        }
    }

    private static boolean hasEnabledStyle() {
        return AmoledTheme.isEnabled() || AccentColor.isSettingsWebViewStyleEnabled();
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

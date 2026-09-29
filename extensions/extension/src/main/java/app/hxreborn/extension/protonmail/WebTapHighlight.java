/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.webkit.WebView;

import app.hxreborn.extension.WebAssets;
import app.morphe.extension.shared.Logger;

@SuppressWarnings("unused")
public final class WebTapHighlight {
    private WebTapHighlight() {}

    public static void remove(WebView view) {
        try {
            if (view == null) return;
            view.evaluateJavascript(WebAssets.TAP_HIGHLIGHT_WEBVIEW, null);
        } catch (Throwable t) {
            Logger.printException(() -> "Could not remove the web view tap highlight", t);
        }
    }
}

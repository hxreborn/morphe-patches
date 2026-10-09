/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import android.webkit.WebView;
import app.morphe.extension.shared.Logger;

import app.hxreborn.extension.WebAssets;

@SuppressWarnings("unused")
public final class WebTapHighlight {

    private WebTapHighlight() {
    }

    public static void remove(WebView view) {
        try {
            if (view == null) {
                return;
            }
            view.evaluateJavascript(WebAssets.TAP_HIGHLIGHT_WEBVIEW, null);
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not remove the web view tap highlight", ex);
        }
    }

}

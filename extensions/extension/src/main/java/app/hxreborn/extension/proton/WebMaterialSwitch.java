/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.webkit.WebView;
import app.morphe.extension.shared.Logger;

import app.hxreborn.extension.WebAssets;

@SuppressWarnings("unused")
public final class WebMaterialSwitch {

    private WebMaterialSwitch() {
    }

    public static void restyleIfEnabled(WebView view) {
        if (MaterialSwitches.isEnabled()) {
            restyle(view);
        }
    }

    public static void restyle(WebView view) {
        try {
            if (view == null) {
                return;
            }
            view.evaluateJavascript(WebAssets.MATERIAL_SWITCH_WEBVIEW, null);
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not restyle the web view switches", ex);
        }
    }

}

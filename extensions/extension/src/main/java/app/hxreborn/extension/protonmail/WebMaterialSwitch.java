/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.webkit.WebView;
import app.morphe.extension.shared.Logger;

import app.hxreborn.extension.WebAssets;
import app.hxreborn.extension.proton.MaterialSwitches;

@SuppressWarnings("unused")
public final class WebMaterialSwitch {

    private WebMaterialSwitch() {
    }

    public static void apply(WebView view) {
        try {
            if (view == null || !MaterialSwitches.isEnabled()) {
                return;
            }
            view.evaluateJavascript(WebAssets.MATERIAL_SWITCH_WEBVIEW, null);
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not restyle the web view switches", ex);
        }
    }

}

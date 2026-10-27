/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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

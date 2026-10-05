/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.content.res.Configuration;
import android.webkit.WebView;

import app.hxreborn.extension.WebAssets;
import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.SwitchStyle;
import app.morphe.extension.shared.Logger;

@SuppressWarnings("unused")
public final class WebMaterialSwitch {
    private WebMaterialSwitch() {}

    public static void apply(WebView view) {
        try {
            if (view == null) return;
            final int nightMode =
                    view.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            final int thumb = SwitchStyle.contentColorOn(
                    AccentColor.getAccentColor(nightMode == Configuration.UI_MODE_NIGHT_YES));
            view.evaluateJavascript(
                    WebAssets.MATERIAL_SWITCH_WEBVIEW.replace(
                            "__CHECKED_THUMB__", String.format("#%06X", thumb & 0xFFFFFF)),
                    null);
        } catch (Throwable t) {
            Logger.printException(() -> "Could not restyle the web view switches", t);
        }
    }
}

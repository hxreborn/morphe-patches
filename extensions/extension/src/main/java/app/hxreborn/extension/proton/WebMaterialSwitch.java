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

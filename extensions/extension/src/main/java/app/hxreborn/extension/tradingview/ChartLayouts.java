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
package app.hxreborn.extension.tradingview;

import java.util.Collections;
import java.util.Set;

import android.util.Log;
import android.webkit.WebView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import app.hxreborn.extension.WebAssets;

@SuppressWarnings("unused")
public final class ChartLayouts {

    private static final String TAG = "hxTradingView";

    private static final Set<String> ANY_ORIGIN_RULES = Collections.singleton("*");

    private ChartLayouts() {
    }

    public static void addLayoutLimitScript(WebView view) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Log.w(TAG, "WebView cannot run document start scripts, chart layout limit unchanged");
            return;
        }
        WebViewCompat.addDocumentStartJavaScript(view, WebAssets.TRADINGVIEW_CHART_LAYOUTS_WEBVIEW, ANY_ORIGIN_RULES);
    }

}

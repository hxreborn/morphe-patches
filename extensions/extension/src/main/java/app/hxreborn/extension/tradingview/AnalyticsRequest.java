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

import java.net.HttpURLConnection;
import java.util.Collections;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

@SuppressWarnings("unused")
public final class AnalyticsRequest {

    private static final String TELEMETRY_HOST = "telemetry.tradingview.com";

    private static final String SNOWPLOW_HOST = "snowplow-pixel.tradingview.com";

    private static final String NO_CONTENT_REASON = "No Content";

    private AnalyticsRequest() {
    }

    public static WebResourceResponse intercept(WebResourceRequest request) {
        String host = request.getUrl().getHost();
        if (!TELEMETRY_HOST.equalsIgnoreCase(host) && !SNOWPLOW_HOST.equalsIgnoreCase(host)) {
            return null;
        }
        return new WebResourceResponse(null, null, HttpURLConnection.HTTP_NO_CONTENT, NO_CONTENT_REASON,
                Collections.emptyMap(), null);
    }

}

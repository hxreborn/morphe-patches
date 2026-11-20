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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsRequestTest {

    @Test
    public void returnsNoContentForAnalyticsHosts() {
        for (String host : new String[] { "telemetry.tradingview.com", "SNOWPLOW-PIXEL.TRADINGVIEW.COM" }) {
            WebResourceResponse response = AnalyticsRequest.intercept(request("https://" + host + "/event"));
            assertNotNull(response);
            assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.getStatusCode());
        }
    }

    @Test
    public void returnsNullForOtherHostsAndHostlessUrls() {
        for (String url : new String[] { "https://www.tradingview.com/chart/",
                "https://telemetry.tradingview.com.example.org/", "https://mobile-sp.tradingview.com/",
                "data:text/plain,test" }) {
            assertNull(AnalyticsRequest.intercept(request(url)));
        }
    }

    private static WebResourceRequest request(String url) {
        return new WebResourceRequest() {
            @Override
            public Uri getUrl() {
                return Uri.parse(url);
            }

            @Override
            public boolean isForMainFrame() {
                return false;
            }

            @Override
            public boolean isRedirect() {
                return false;
            }

            @Override
            public boolean hasGesture() {
                return false;
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public Map<String, String> getRequestHeaders() {
                return Collections.emptyMap();
            }
        };
    }

}

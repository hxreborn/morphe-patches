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
package app.hxreborn.extension.raindrop;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import android.webkit.CookieManager;
import org.json.JSONException;
import org.json.JSONObject;

import app.hxreborn.extension.shared.Streams;

final class RaindropApi {

    static final String API_URL = "https://api.raindrop.io/v1/";

    private static final int TIMEOUT_MILLIS = 15000;

    private RaindropApi() {
    }

    static JSONObject get(String path) throws IOException, JSONException {
        return new JSONObject(new String(getBytes(path), StandardCharsets.UTF_8));
    }

    static byte[] getBytes(String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL + path).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            String cookies = CookieManager.getInstance().getCookie(API_URL);
            if (cookies != null) {
                connection.setRequestProperty("Cookie", cookies);
            }
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("GET " + path + " returned HTTP " + code);
            }
            try (InputStream input = connection.getInputStream()) {
                return Streams.readAll(input);
            }
        } finally {
            connection.disconnect();
        }
    }

}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import android.webkit.CookieManager;
import org.json.JSONException;
import org.json.JSONObject;

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
                return readAll(input);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

}

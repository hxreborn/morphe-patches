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
package app.hxreborn.extension.muvizwidgets;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class WeatherBackend {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search?count=10&language=en&format=json&name=";

    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast?current=temperature_2m,weather_code,is_day&timezone=auto";

    private static final String REVERSE_GEOCODING_URL = "https://api.bigdatacloud.net/data/reverse-geocode-client?localityLanguage=en";

    private static final String NAMES_FILE = "hx_muviz_weather_names";

    private static final String CITIES_PATH = "/cities/";

    private static final String CURRENT_PATH = "/current/";

    private static final int TIMEOUT_MILLIS = 4000;

    private static final int SERVER_BACKLOG = 16;

    private static final int MILLIS_PER_SECOND = 1000;

    private static final int READ_BUFFER_BYTES = 4096;

    private static ServerSocket server;

    private static Context applicationContext;

    private static SharedPreferences cityNames;

    private WeatherBackend() {
    }

    public static synchronized void attach(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public static synchronized String baseUrl() {
        if (server == null) {
            try {
                cityNames = applicationContext.getSharedPreferences(NAMES_FILE, Context.MODE_PRIVATE);
                server = new ServerSocket(0, SERVER_BACKLOG, InetAddress.getByName("127.0.0.1"));
            } catch (IOException ex) {
                throw new IllegalStateException("Weather backend did not start", ex);
            }
            final ServerSocket socket = server;
            final Thread acceptor = new Thread(() -> acceptConnections(socket), "hx-weather-backend");
            acceptor.setDaemon(true);
            acceptor.start();
        }
        return "http://127.0.0.1:" + server.getLocalPort() + "/";
    }

    private static void acceptConnections(ServerSocket socket) {
        while (!socket.isClosed()) {
            try {
                final Socket connection = socket.accept();
                final Thread handler = new Thread(() -> respond(connection), "hx-weather-request");
                handler.setDaemon(true);
                handler.start();
            } catch (IOException ex) {
                return;
            }
        }
    }

    private static void respond(Socket connection) {
        try (Socket socket = connection) {
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            final String requestLine = reader.readLine();
            String line = requestLine;
            while (line != null && !line.isEmpty()) {
                line = reader.readLine();
            }
            int status = HttpURLConnection.HTTP_OK;
            String body;
            try {
                body = route(requestLine.split(" ")[1]);
            } catch (Exception ex) {
                status = HttpURLConnection.HTTP_BAD_GATEWAY;
                body = "{}";
            }
            final byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            final OutputStream out = socket.getOutputStream();
            out.write(("HTTP/1.1 " + status + " X\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "
                    + payload.length + "\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
            out.write(payload);
            out.flush();
        } catch (IOException | RuntimeException ex) {
            return;
        }
    }

    private static String route(String target) throws Exception {
        final String path = target.split("\\?")[0];
        if (path.startsWith(CITIES_PATH)) {
            return findCities(decode(path.substring(CITIES_PATH.length())));
        }
        if (path.startsWith(CURRENT_PATH)) {
            return currentWeather(decode(path.substring(CURRENT_PATH.length())));
        }
        throw new IOException("Unknown path " + path);
    }

    private static String decode(String segment) throws IOException {
        return URLDecoder.decode(segment.replace("+", "%2B"), "UTF-8");
    }

    private static String findCities(String query) throws Exception {
        if (WeatherJson.isCoordinatePair(query)) {
            final String[] parts = query.split(",");
            final double latitude = Double.parseDouble(parts[0]);
            final double longitude = Double.parseDouble(parts[1]);
            final JSONObject city = WeatherJson.reverseCity(reverseGeocode(parts), latitude, longitude);
            rememberName(city);
            return new JSONArray().put(city).toString();
        }
        final JSONArray matches = WeatherJson.cities(httpGet(GEOCODING_URL + URLEncoder.encode(query, "UTF-8")));
        for (int i = 0; i < matches.length(); i++) {
            rememberName(matches.getJSONObject(i));
        }
        return matches.toString();
    }

    private static String currentWeather(String query) throws Exception {
        final String[] parts = query.split(",");
        final double latitude = Double.parseDouble(parts[0]);
        final double longitude = Double.parseDouble(parts[1]);
        final String forecast = httpGet(FORECAST_URL + "&latitude=" + parts[0] + "&longitude=" + parts[1]);
        String name = cityNames.getString(WeatherJson.coordinateKey(latitude, longitude), null);
        if (name == null) {
            name = WeatherJson.reverseName(reverseGeocode(parts));
        }
        return WeatherJson.current(forecast, name, System.currentTimeMillis() / MILLIS_PER_SECOND).toString();
    }

    private static String reverseGeocode(String[] coordinates) throws IOException {
        return httpGet(REVERSE_GEOCODING_URL + "&latitude=" + coordinates[0] + "&longitude=" + coordinates[1]);
    }

    private static void rememberName(JSONObject city) throws Exception {
        final String key = WeatherJson.coordinateKey(city.getDouble("lat"), city.getDouble("lon"));
        cityNames.edit().putString(key, city.getString("name")).apply();
    }

    private static String httpGet(String address) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "HTTP " + connection.getResponseCode() + " from " + connection.getURL().getHost());
            }
            try (InputStream in = connection.getInputStream()) {
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                final byte[] chunk = new byte[READ_BUFFER_BYTES];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                return buffer.toString("UTF-8");
            }
        } finally {
            connection.disconnect();
        }
    }

}

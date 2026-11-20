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
package app.hxreborn.extension.quranify;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Set;

import android.util.Log;
import org.json.JSONException;

@SuppressWarnings("unused")
public final class LocalApiServer {

    static final String LOG_TAG = "hxQuranify";

    static final int TIMEOUT_MS = 30000;

    private static final String LOOPBACK_ADDRESS = "127.0.0.1";

    private static final String API_PATH_PREFIX = "/api/";

    private static final String ANDROID_ORIGIN = "https://api-android.quranify.pro";

    private static final Set<String> WEB_ONLY_PATHS = Set.of("/api/v2/reciters");

    private static final int SERVER_BACKLOG = 16;

    private static LocalApiServer instance;

    private final ServerSocket server;

    private final WebApiSession webSession = new WebApiSession();

    private LocalApiServer() throws IOException {
        this.server = new ServerSocket(0, SERVER_BACKLOG, InetAddress.getByName(LOOPBACK_ADDRESS));
        startDaemon(this::acceptConnections, "Quranify API");
    }

    public static synchronized String baseUrl() {
        if (instance == null) {
            try {
                instance = new LocalApiServer();
            } catch (IOException ex) {
                throw new IllegalStateException("Could not start Quranify API server", ex);
            }
        }
        return "http://" + LOOPBACK_ADDRESS + ":" + instance.server.getLocalPort() + API_PATH_PREFIX;
    }

    private static void startDaemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private void acceptConnections() {
        while (!this.server.isClosed()) {
            try {
                Socket socket = this.server.accept();
                startDaemon(() -> this.serve(socket), "Quranify API request");
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Could not accept API request", ex);
                return;
            }
        }
    }

    private void serve(Socket socket) {
        try (Socket connection = socket) {
            connection.setSoTimeout(TIMEOUT_MS);
            ApiRequest request = ApiRequest.read(connection.getInputStream());
            if (!request.path.startsWith(API_PATH_PREFIX)) {
                throw new IOException("API request outside " + API_PATH_PREFIX);
            }
            this.forward(request).writeTo(connection.getOutputStream());
        } catch (IOException | JSONException | GeneralSecurityException | IllegalArgumentException ex) {
            Log.w(LOG_TAG, "Quranify API request failed");
        }
    }

    private ApiResponse forward(ApiRequest request) throws IOException, JSONException, GeneralSecurityException {
        if (WEB_ONLY_PATHS.contains(request.path)) {
            return this.webSession.send(request);
        }
        return HttpExchange.send(ANDROID_ORIGIN + request.path + request.query, request.method, request.headers,
                request.body);
    }

}

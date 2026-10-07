/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class FakeRaindropApi {

    private static final Map<String, Response> RESPONSES = new ConcurrentHashMap<>();

    private static final Response UNAUTHORIZED = new Response(HttpURLConnection.HTTP_UNAUTHORIZED,
            "{\"result\":false}".getBytes(StandardCharsets.UTF_8), false);

    private static volatile String lastCookie;

    private static boolean isInstalled;

    private FakeRaindropApi() {
    }

    static synchronized void install() {
        if (isInstalled) {
            return;
        }
        URL.setURLStreamHandlerFactory((protocol) -> "https".equals(protocol) ? new Handler() : null);
        isInstalled = true;
    }

    static void reset() {
        RESPONSES.clear();
        lastCookie = null;
    }

    static void respond(String path, String body) {
        respond(path, HttpURLConnection.HTTP_OK, body.getBytes(StandardCharsets.UTF_8));
    }

    static void respond(String path, int status, byte[] body) {
        RESPONSES.put(path, new Response(status, body, false));
    }

    static void respondOneByteAtATime(String path, byte[] body) {
        RESPONSES.put(path, new Response(HttpURLConnection.HTTP_OK, body, true));
    }

    static String lastCookie() {
        return lastCookie;
    }

    private static final class Handler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            String address = url.toString();
            if (!address.startsWith(RaindropApi.API_URL)) {
                throw new IOException("No network in tests: " + address);
            }
            Response response = RESPONSES.get(address.substring(RaindropApi.API_URL.length()));
            return new Connection(url, (response != null) ? response : UNAUTHORIZED);
        }

    }

    private static final class Connection extends HttpURLConnection {

        private final Response response;

        Connection(URL url, Response response) {
            super(url);
            this.response = response;
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public int getResponseCode() {
            lastCookie = getRequestProperty("Cookie");
            return this.response.status;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (this.response.status != HTTP_OK) {
                throw new IOException("HTTP " + this.response.status);
            }
            return this.response.isOneByteAtATime ? new OneByteAtATime(this.response.body)
                    : new ByteArrayInputStream(this.response.body);
        }

    }

    private static final class OneByteAtATime extends InputStream {

        private final byte[] bytes;

        private int position;

        OneByteAtATime(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int read() {
            return (this.position < this.bytes.length) ? (this.bytes[this.position++] & 0xff) : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (this.position >= this.bytes.length) {
                return -1;
            }
            buffer[offset] = this.bytes[this.position++];
            return 1;
        }

    }

    private static final class Response {

        private final int status;

        private final byte[] body;

        private final boolean isOneByteAtATime;

        Response(int status, byte[] body, boolean isOneByteAtATime) {
            this.status = status;
            this.body = body;
            this.isOneByteAtATime = isOneByteAtATime;
        }

    }

}

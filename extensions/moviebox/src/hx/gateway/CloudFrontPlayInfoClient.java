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
package hx.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import hx.platform.InstalledApp;
import hx.util.Logs;
import hx.util.Streams;
import org.json.JSONException;
import org.json.JSONObject;

public final class CloudFrontPlayInfoClient implements PlayInfoLookup {

    private static final String[] API_HOSTS = { "api6.aoneroom.com", "api4sg.aoneroom.com", "api5.aoneroom.com",
            "api4.aoneroom.com", "api3.aoneroom.com" };

    private static final String VISITOR_LOGIN = "/wefeed-mobile-bff/user-api/visitor-login";

    private static final int REQUEST_TIMEOUT_MS = 5000;

    private static final long LOOKUP_BUDGET_MS = 5000L;

    private static final long RETRY_COOLDOWN_MS = 10 * 60 * 1000L;

    private final GatewayHeaders gatewayHeaders;

    private final ConcurrentMap<String, String> tokens = new ConcurrentHashMap<>();

    private final Map<String, Long> hostRetryAtMs = new ConcurrentHashMap<>();

    public CloudFrontPlayInfoClient(BuildTag buildTag) {
        this.gatewayHeaders = new GatewayHeaders(ClientInfo.forInstalledApp(), buildTag);
    }

    @Override
    public String fetchPlayInfo(String pathAndQuery, PlayInfoAcceptance acceptance) {
        if (SigningKey.VALUE == null) {
            return null;
        }
        long startedMs = SystemClock.elapsedRealtime();
        List<String> hosts = hostsReadyAt(startedMs);
        if (hosts.isEmpty()) {
            Log.i(Logs.TAG, "play-info: every API host is cooling down, staying on Edge-Cache");
            return null;
        }
        BlockingQueue<Answer> answers = startLookups(hosts, pathAndQuery, acceptance, startedMs);
        Set<String> unanswered = new HashSet<>(hosts);
        String body = awaitAccepted(answers, unanswered, startedMs + LOOKUP_BUDGET_MS);
        if (body != null) {
            return body;
        }
        for (String host : unanswered) {
            coolDown(host);
        }
        Log.i(Logs.TAG, "play-info: no unpaced CDN from " + hosts + " within " + LOOKUP_BUDGET_MS
                + " ms, staying on Edge-Cache");
        return null;
    }

    private List<String> hostsReadyAt(long nowMs) {
        List<String> hosts = new ArrayList<>();
        for (String host : API_HOSTS) {
            Long retryAtMs = this.hostRetryAtMs.get(host);
            if (retryAtMs == null || nowMs >= retryAtMs) {
                hosts.add(host);
            }
        }
        return hosts;
    }

    private BlockingQueue<Answer> startLookups(List<String> hosts, String pathAndQuery,
            PlayInfoAcceptance acceptance, long startedMs) {
        BlockingQueue<Answer> answers = new LinkedBlockingQueue<>();
        for (String host : hosts) {
            Thread worker = new Thread(
                    () -> answers.add(new Answer(host, fetchAccepted(host, pathAndQuery, acceptance, startedMs))),
                    "hx-play-info-" + host);
            worker.setDaemon(true);
            worker.start();
        }
        return answers;
    }

    private static String awaitAccepted(BlockingQueue<Answer> answers, Set<String> unanswered, long deadlineMs) {
        try {
            while (!unanswered.isEmpty()) {
                long remainingMs = deadlineMs - SystemClock.elapsedRealtime();
                Answer answer = (remainingMs > 0) ? answers.poll(remainingMs, TimeUnit.MILLISECONDS) : null;
                if (answer == null) {
                    return null;
                }
                unanswered.remove(answer.host);
                if (answer.body != null) {
                    return answer.body;
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void coolDown(String host) {
        this.hostRetryAtMs.put(host, SystemClock.elapsedRealtime() + RETRY_COOLDOWN_MS);
    }

    private String fetchAccepted(String host, String pathAndQuery, PlayInfoAcceptance acceptance,
            long startedMs) {
        String token = null;
        try {
            token = visitorToken(host);
            String body = sendRequest("GET", host, pathAndQuery, null, token);
            long elapsedMs = SystemClock.elapsedRealtime() - startedMs;
            if (acceptance.accepts(body)) {
                if (elapsedMs <= LOOKUP_BUDGET_MS) {
                    this.hostRetryAtMs.remove(host);
                }
                Log.i(Logs.TAG, "play-info from " + host + ": unpaced CDN instead of Edge-Cache, " + elapsedMs + " ms");
                return body;
            }
            Log.i(Logs.TAG, "play-info from " + host + " not used, " + elapsedMs + " ms");
            return null;
        } catch (Throwable ex) {
            if (token != null) {
                this.tokens.remove(host, token);
            }
            Log.w(Logs.TAG,
                    "play-info from " + host + " failed after " + (SystemClock.elapsedRealtime() - startedMs) + " ms",
                    ex);
        }
        coolDown(host);
        return null;
    }

    private String visitorToken(String host) throws IOException, JSONException {
        String token = this.tokens.get(host);
        if (token != null) {
            return token;
        }
        String login = sendRequest("POST", host, VISITOR_LOGIN, "{}", null);
        token = new JSONObject(login).getJSONObject("data").getString("token");
        this.tokens.put(host, token);
        return token;
    }

    private String sendRequest(String method, String host, String pathAndQuery, String body, String token)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + host + pathAndQuery).openConnection();
        try {
            connection.setConnectTimeout(REQUEST_TIMEOUT_MS);
            connection.setReadTimeout(REQUEST_TIMEOUT_MS);
            connection.setRequestMethod(method);
            addHeaders(connection, method, pathAndQuery, body, token);
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            return readSuccessful(connection, host);
        } finally {
            connection.disconnect();
        }
    }

    private void addHeaders(HttpURLConnection connection, String method, String pathAndQuery, String body, String token)
            throws IOException {
        Map<String, String> headers = this.gatewayHeaders.create(SigningKey.VALUE, method, pathAndQuery, body, token);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
    }

    private static String readSuccessful(HttpURLConnection connection, String host) throws IOException {
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " from " + host);
        }
        try (InputStream in = connection.getInputStream()) {
            return new String(Streams.readAll(in), StandardCharsets.UTF_8);
        }
    }

    private static final class Answer {

        final String host;

        final String body;

        Answer(String host, String body) {
            this.host = host;
            this.body = body;
        }

    }

    private static final class SigningKey {

        static final byte[] VALUE = read();

        private static final String META_DATA_KEY = "gateway_secret_online";

        private static byte[] read() {
            try {
                Context context = InstalledApp.context();
                Bundle metaData = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
                String declared = (metaData != null) ? metaData.getString(META_DATA_KEY) : null;
                if (declared != null) {
                    return Base64.decode(declared, Base64.DEFAULT);
                }
                Log.w(Logs.TAG, META_DATA_KEY + " missing from the manifest, staying on Edge-Cache");
            } catch (Exception ex) {
                Log.w(Logs.TAG, META_DATA_KEY + " unreadable, staying on Edge-Cache", ex);
            }
            return null;
        }

    }

}

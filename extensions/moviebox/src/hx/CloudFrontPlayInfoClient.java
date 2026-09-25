package hx;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

final class CloudFrontPlayInfoClient {
    private static final String TAG = "hxreborn/moviebox";
    private static final String[] API_HOSTS = {
            "api6.aoneroom.com", "api4sg.aoneroom.com", "api5.aoneroom.com", "api4.aoneroom.com", "api3.aoneroom.com"};
    private static final String WITHHELD_CLIENT_ADDRESS = "unknown";
    private static final String SIGNING_KEY_META_DATA = "gateway_secret_online";
    private static final String FALLBACK_SIGNING_KEY = "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O";
    private static final String VISITOR_LOGIN = "/wefeed-mobile-bff/user-api/visitor-login";
    private static final String JSON_TYPE = "application/json";
    private static final int REQUEST_TIMEOUT_MS = 5000;
    private static final long LOOKUP_BUDGET_MS = 5000L;
    private static final long RETRY_COOLDOWN_MS = 10 * 60 * 1000L;

    private final String clientInfo;
    private volatile byte[] signingKey;
    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> hostRetryAtMs = new ConcurrentHashMap<>();

    CloudFrontPlayInfoClient() {
        try {
            clientInfo = new JSONObject()
                    .put("device_id", UUID.randomUUID().toString().replace("-", ""))
                    .put("os_version", Build.VERSION.RELEASE)
                    .toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    String fetchPlayInfo(String pathAndQuery, Predicate<String> accepted) {
        long startedMs = SystemClock.elapsedRealtime();
        List<String> hosts = new ArrayList<>();
        for (String host : API_HOSTS) {
            Long retryAtMs = hostRetryAtMs.get(host);
            if (retryAtMs == null || startedMs >= retryAtMs) hosts.add(host);
        }
        if (hosts.isEmpty()) {
            Log.i(TAG, "play-info: every API host is cooling down, staying on Edge-Cache");
            return null;
        }
        BlockingQueue<Answer> answers = new LinkedBlockingQueue<>();
        for (String host : hosts) {
            Thread worker = new Thread(
                    () -> answers.add(new Answer(host, fetchAccepted(host, pathAndQuery, accepted, startedMs))),
                    "hx-play-info-" + host);
            worker.setDaemon(true);
            worker.start();
        }
        Set<String> unanswered = new HashSet<>(hosts);
        long deadlineMs = startedMs + LOOKUP_BUDGET_MS;
        try {
            while (!unanswered.isEmpty()) {
                long remainingMs = deadlineMs - SystemClock.elapsedRealtime();
                Answer answer = remainingMs > 0 ? answers.poll(remainingMs, TimeUnit.MILLISECONDS) : null;
                if (answer == null) break;
                unanswered.remove(answer.host);
                if (answer.body != null) return answer.body;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (String host : unanswered) coolDown(host);
        Log.i(TAG, "play-info: no unpaced CDN from " + hosts + " within " + LOOKUP_BUDGET_MS
                + " ms, staying on Edge-Cache");
        return null;
    }

    private void coolDown(String host) {
        hostRetryAtMs.put(host, SystemClock.elapsedRealtime() + RETRY_COOLDOWN_MS);
    }

    private String fetchAccepted(String host, String pathAndQuery, Predicate<String> accepted, long startedMs) {
        String token = null;
        try {
            token = visitorToken(host);
            String body = sendRequest("GET", host, pathAndQuery, null, token);
            long elapsedMs = SystemClock.elapsedRealtime() - startedMs;
            if (accepted.test(body)) {
                if (elapsedMs <= LOOKUP_BUDGET_MS) hostRetryAtMs.remove(host);
                Log.i(TAG, "play-info from " + host + ": unpaced CDN instead of Edge-Cache, " + elapsedMs + " ms");
                return body;
            }
            Log.i(TAG, "play-info from " + host + " not used, " + elapsedMs + " ms");
            return null;
        } catch (Throwable e) {
            if (token != null) tokens.remove(host, token);
            Log.w(TAG, "play-info from " + host + " failed after "
                    + (SystemClock.elapsedRealtime() - startedMs) + " ms", e);
        }
        coolDown(host);
        return null;
    }

    private static final class Answer {
        final String host;
        final String body;

        Answer(String host, String body) {
            this.host = host;
            this.body = body;
        }
    }

    private String visitorToken(String host) throws IOException, JSONException {
        String token = tokens.get(host);
        if (token != null) return token;
        token = new JSONObject(sendRequest("POST", host, VISITOR_LOGIN, "{}", null)).getJSONObject("data").getString("token");
        tokens.put(host, token);
        return token;
    }

    private String sendRequest(String method, String host, String pathAndQuery, String body, String token)
            throws IOException {
        long timestamp = System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + host + pathAndQuery).openConnection();
        connection.setConnectTimeout(REQUEST_TIMEOUT_MS);
        connection.setReadTimeout(REQUEST_TIMEOUT_MS);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", JSON_TYPE);
        connection.setRequestProperty("Content-Type", JSON_TYPE);
        connection.setRequestProperty("x-client-token", timestamp + "," + md5Hex(
                new StringBuilder(String.valueOf(timestamp)).reverse().toString().getBytes(StandardCharsets.UTF_8)));
        connection.setRequestProperty("x-tr-signature", createSignature(signingKey(), method, pathAndQuery, body, timestamp));
        connection.setRequestProperty("x-client-info", clientInfo);
        connection.setRequestProperty("x-forwarded-for", WITHHELD_CLIENT_ADDRESS);
        if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
        try {
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " from " + host);
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                for (int count; (count = in.read(buffer)) > 0; ) bytes.write(buffer, 0, count);
                return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private byte[] signingKey() {
        byte[] key = signingKey;
        if (key != null) return key;
        String encoded = FALLBACK_SIGNING_KEY;
        try {
            Context context = (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            Bundle metaData = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            String declared = metaData == null ? null : metaData.getString(SIGNING_KEY_META_DATA);
            if (declared != null) encoded = declared;
            else Log.w(TAG, SIGNING_KEY_META_DATA + " missing from the manifest, using the built-in key");
        } catch (Exception e) {
            Log.w(TAG, SIGNING_KEY_META_DATA + " unreadable, using the built-in key", e);
        }
        key = Base64.decode(encoded, Base64.DEFAULT);
        signingKey = key;
        return key;
    }

    private static String createSignature(byte[] signingKey, String method, String pathAndQuery, String body,
                                          long timestamp) throws IOException {
        byte[] bodyBytes = body == null ? null : body.getBytes(StandardCharsets.UTF_8);
        String canonical = method + "\n" + JSON_TYPE + "\n" + JSON_TYPE + "\n"
                + (bodyBytes == null ? "" : String.valueOf(bodyBytes.length)) + "\n"
                + timestamp + "\n"
                + (bodyBytes == null ? "" : md5Hex(bodyBytes)) + "\n"
                + canonicalPath(pathAndQuery);
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(signingKey, "HmacMD5"));
            byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return timestamp + "|2|" + Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    private static String canonicalPath(String pathAndQuery) throws IOException {
        int question = pathAndQuery.indexOf('?');
        if (question < 0) return pathAndQuery;
        List<String> pairs = new ArrayList<>();
        for (String pair : pathAndQuery.substring(question + 1).split("&")) {
            if (!pair.isEmpty()) pairs.add(decode(pair));
        }
        Collections.sort(pairs, (a, b) -> queryKey(a).compareTo(queryKey(b)));
        return pathAndQuery.substring(0, question + 1) + String.join("&", pairs);
    }

    private static String decode(String pair) throws IOException {
        int equals = pair.indexOf('=');
        if (equals < 0) return URLDecoder.decode(pair, "UTF-8");
        return URLDecoder.decode(pair.substring(0, equals), "UTF-8") + "="
                + URLDecoder.decode(pair.substring(equals + 1), "UTF-8");
    }

    private static String queryKey(String pair) {
        int equals = pair.indexOf('=');
        return equals < 0 ? pair : pair.substring(0, equals);
    }

    private static String md5Hex(byte[] bytes) throws IOException {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte b : MessageDigest.getInstance("MD5").digest(bytes)) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
}

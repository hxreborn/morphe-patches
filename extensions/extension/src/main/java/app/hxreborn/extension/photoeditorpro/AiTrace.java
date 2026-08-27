/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class AiTrace {

    private static final String TAG = "hxreborn/ai";
    private static final int MAX_SESSIONS = 8;
    private static final int MAX_EXCHANGES_PER_SESSION = 16;
    private static final int MAX_ATTEMPTS_PER_EXCHANGE = 250;
    private static final int MAX_HEADERS = 32;
    private static final int MAX_PAYLOAD_FIELDS = 24;
    private static final int MAX_VALUE_CHARS = 512;
    private static final int MAX_BODY_BYTES = 8192;
    private static final int MAX_BODY_CHARS = 2048;

    private static final String[] ROUTING_SEGMENTS = {
            "api", "new", "predict", "query", "result", "results", "revoke", "task", "upload",
    };

    private static final String[][] LABELS = {
            {"inpaint", "AI Remove"},
            {"remove", "AI Remove"},
            {"expand", "AI Expand"},
            {"outpaint", "AI Expand"},
            {"enhance", "Enhance"},
            {"color", "Enhance"},
            {"sketch", "Sketch"},
            {"segmentation", "Cutout"},
            {"eraser", "Cutout"},
            {"bgeraser", "Cutout"},
            {"subject", "Cutout"},
    };

    private static final String[] SECRET_TOKENS = {
            "token", "secret", "signature", "sign", "auth", "cookie", "password", "session",
    };

    private static final String POLISH_PREFIX = "polish_";
    private static final String PUT_FILE = "putFile";
    private static final String GET = "GET";

    private static final Set<String> CONTROL_OPERATIONS = Set.of("revoke", "cancel", "query", "task");

    private static final Pattern UUID = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final ThreadLocal<SimpleDateFormat> ISO = new ThreadLocal<>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        }
    };

    private static final Object LOCK = new Object();
    private static final List<Session> SESSIONS = new ArrayList<>();

    private AiTrace() {
    }

    private static boolean enabled() {
        try {
            return PatchSettings.LOG_ENDPOINTS.get();
        } catch (Exception ex) {
            return false;
        }
    }

    public static void install(OkHttpClient.Builder builder) {
        builder.addInterceptor(AiTrace::trace);
    }

    private static Response trace(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        if (!enabled()) {
            return chain.proceed(request);
        }

        String url = request.url().toString();
        Attempt attempt = open(request, url);
        try {
            Response response = chain.proceed(request);
            close(attempt, response);
            return response;
        } catch (IOException | RuntimeException ex) {
            fail(attempt, ex);
            throw ex;
        }
    }

    private static Attempt open(Request request, String url) {
        long now = System.currentTimeMillis();
        String method = request.method();
        Attempt attempt = new Attempt(now, 0);
        try {
            synchronized (LOCK) {
                Session session = GET.equals(method)
                        ? sessionFor(operationOf(url), resourceOf(url), now)
                        : pendingSession(operationOf(url), now);
                Exchange exchange = session.exchange(method, url);
                if (exchange.payload.isEmpty()) {
                    exchange.payload = payloadOf(request.body());
                }
                exchange.add(attempt);
                session.touch(now);
            }
            ProgressTrace.refreshLive();
        } catch (Exception ex) {
            Log.w(TAG, "open", ex);
        }
        return attempt;
    }

    private static void close(Attempt attempt, Response response) {
        try {
            synchronized (LOCK) {
                describe(response, attempt);
            }
            Log.i(TAG, String.format(Locale.US, "%s  %s %s -> %s  %dms",
                    iso(attempt.startedAtMs), response.request().method(),
                    path(response.request().url().toString()), attempt.statusLine(),
                    attempt.durationMs()));
            if (!attempt.body.isEmpty()) {
                Log.i(TAG, "    body: " + attempt.body);
            }
            ProgressTrace.refreshLive();
        } catch (Exception ex) {
            Log.w(TAG, "close", ex);
        }
    }

    private static void fail(Attempt attempt, Exception cause) {
        try {
            synchronized (LOCK) {
                attempt.endedAtMs = System.currentTimeMillis();
                attempt.message = clamp(String.valueOf(cause));
            }
            ProgressTrace.refreshLive();
        } catch (Exception ex) {
            Log.w(TAG, "fail", ex);
        }
    }

    public static void upload(Object storageReference) {
        if (!enabled()) {
            return;
        }
        try {
            String url = String.valueOf(storageReference);
            long now = System.currentTimeMillis();
            synchronized (LOCK) {
                Session session = pendingSession(operationOf(url), now);
                session.exchange(PUT_FILE, url).add(new Attempt(now, now));
                session.touch(now);
            }
            Log.i(TAG, iso(now) + "  " + PUT_FILE + " " + path(url));
            ProgressTrace.refreshLive();
        } catch (Exception ex) {
            Log.w(TAG, "upload", ex);
        }
    }

    private static void describe(Response response, Attempt attempt) {
        attempt.endedAtMs = System.currentTimeMillis();
        attempt.statusCode = response.code();
        attempt.message = clamp(response.message());
        attempt.protocol = response.protocol().toString();
        attempt.responseHeaders = headersOf(response.headers());
        attempt.requestHeaders = headersOf(response.request().headers());

        long sent = response.sentRequestAtMillis();
        long received = response.receivedResponseAtMillis();
        if (sent > 0 && received >= sent) {
            attempt.startedAtMs = sent;
            attempt.endedAtMs = received;
        }

        ResponseBody body = response.body();
        if (body != null) {
            attempt.contentLength = body.contentLength();
            MediaType type = body.contentType();
            attempt.contentType = type == null ? "" : type.toString();
        }

        attempt.body = peek(response);
    }

    private static String peek(Response response) {
        try {
            String text = response.peekBody(MAX_BODY_BYTES).string().replace('\n', ' ');
            return text.length() <= MAX_BODY_CHARS
                    ? text
                    : text.substring(0, MAX_BODY_CHARS) + "\u2026";
        } catch (Exception ex) {
            return "";
        }
    }

    private static List<String[]> headersOf(Headers headers) {
        int count = Math.min(headers.size(), MAX_HEADERS);
        List<String[]> pairs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pairs.add(new String[]{headers.name(i), redact(headers.name(i), headers.value(i))});
        }
        return pairs;
    }

    private static List<String[]> payloadOf(RequestBody body) {
        if (!(body instanceof FormBody form)) {
            return new ArrayList<>();
        }
        int count = Math.min(form.size(), MAX_PAYLOAD_FIELDS);
        List<String[]> pairs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pairs.add(new String[]{form.name(i), redact(form.name(i), form.value(i))});
        }
        return pairs;
    }

    private static String redact(String name, String value) {
        String key = name.toLowerCase(Locale.US);
        for (String secret : SECRET_TOKENS) {
            if (key.contains(secret)) {
                return value.isEmpty() ? "" : "<redacted, " + value.length() + " chars>";
            }
        }
        return clamp(value);
    }

    private static String clamp(String value) {
        return value.length() <= MAX_VALUE_CHARS ? value : value.substring(0, MAX_VALUE_CHARS) + "…";
    }

    public static List<Session> sessions() {
        synchronized (LOCK) {
            return new ArrayList<>(SESSIONS);
        }
    }

    public static String headline() {
        synchronized (LOCK) {
            if (SESSIONS.isEmpty()) {
                return enabled()
                        ? "No AI request recorded yet this session"
                        : "Turn on AI request logging above to record requests";
            }
            Session last = null;
            for (int i = SESSIONS.size() - 1; i >= 0 && last == null; i--) {
                if (!SESSIONS.get(i).control) {
                    last = SESSIONS.get(i);
                }
            }
            if (last == null) {
                last = SESSIONS.get(SESSIONS.size() - 1);
            }
            return String.format(Locale.US, "%s · %d requests · %s",
                    last.feature, last.requestCount(), duration(last.durationMs()));
        }
    }

    static List<LiveRow> liveRows(long sinceEpochMs, int max) {
        List<LiveRow> rows = new ArrayList<>();
        try {
            synchronized (LOCK) {
                Map<String, LiveRow> byRoute = new LinkedHashMap<>();
                for (Session session : SESSIONS) {
                    if (session.control) {
                        continue;
                    }
                    for (Exchange exchange : session.exchanges) {
                        for (Attempt attempt : exchange.attempts) {
                            if (attempt.startedAtMs >= sinceEpochMs) {
                                fold(byRoute, exchange, attempt);
                            }
                        }
                    }
                }
                rows.addAll(byRoute.values());
                while (rows.size() > max) {
                    rows.remove(0);
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "liveRows", ex);
        }
        return rows;
    }

    private static void fold(Map<String, LiveRow> byRoute, Exchange exchange, Attempt attempt) {
        String path = overlayPath(exchange.url);
        String status = overlayStatus(attempt);
        LiveRow row = byRoute.get(exchange.method + ' ' + path);
        if (row == null) {
            byRoute.put(exchange.method + ' ' + path,
                    new LiveRow(exchange.method, path, status, attempt.statusCode));
            return;
        }
        row.count++;
        if (status != null && !status.isEmpty()) {
            row.status = status;
            row.statusCode = attempt.statusCode;
        }
    }

    private static String overlayStatus(Attempt attempt) {
        if (attempt.dispatchOnly()) {
            return ProgressTrace.uploading() ? "" : null;
        }
        if (attempt.inFlight() || HttpStatus.isPending(attempt.statusCode)) {
            return "";
        }
        String reason = HttpStatus.reason(attempt.statusCode);
        return reason.isEmpty() ? attempt.status() : attempt.status() + " " + reason;
    }

    private static String overlayPath(String url) {
        List<String> kept = new ArrayList<>();
        for (String segment : path(url).split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (segment.indexOf('.') >= 0) {
                kept.add(shorten(segment));
                continue;
            }
            if (operationToken(segment) != null) {
                kept.add(segment);
            }
        }
        if (kept.isEmpty()) {
            return path(url);
        }
        StringBuilder out = new StringBuilder();
        for (int i = Math.max(0, kept.size() - 2); i < kept.size(); i++) {
            out.append('/').append(kept.get(i));
        }
        return out.toString();
    }

    private static String shorten(String segment) {
        if (segment.length() <= 20) {
            return segment;
        }
        return segment.substring(0, 8) + "…" + segment.substring(segment.length() - 8);
    }

    static String safeUrl(String url) {
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query) + "?<redacted>";
    }

    static String iso(long epochMs) {
        return ISO.get().format(new Date(epochMs));
    }

    static String duration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        return String.format(Locale.US, "%.2fs", millis / 1000f);
    }

    private static Session pendingSession(String operation, long startedAtMs) {
        for (int i = SESSIONS.size() - 1; i >= 0; i--) {
            Session session = SESSIONS.get(i);
            if (session.operation.equals(operation) && session.resource == null) {
                return session;
            }
        }
        return openSession(operation, startedAtMs);
    }

    private static Session openSession(String operation, long startedAtMs) {
        Session session = new Session(operation, startedAtMs);
        session.control = CONTROL_OPERATIONS.contains(operation);
        SESSIONS.add(session);
        while (SESSIONS.size() > MAX_SESSIONS) {
            SESSIONS.remove(0);
        }
        return session;
    }

    private static Session sessionFor(String operation, String resource, long startedAtMs) {
        for (int i = SESSIONS.size() - 1; i >= 0; i--) {
            Session session = SESSIONS.get(i);
            if (!session.operation.equals(operation)) {
                continue;
            }
            if (resource != null && resource.equals(session.resource)) {
                return session;
            }
            if (session.resource == null) {
                session.resource = resource;
                return session;
            }
        }
        Session session = openSession(operation, startedAtMs);
        session.resource = resource;
        session.postObserved = false;
        return session;
    }

    static String operationOf(String url) {
        String first = null;
        String fallback = null;
        for (String segment : path(url).split("/")) {
            String token = operationToken(segment);
            if (token != null) {
                if (segment.toLowerCase(Locale.US).startsWith(POLISH_PREFIX)) {
                    return token;
                }
                if (first == null) {
                    first = token;
                }
            } else if (namedSegment(segment)) {
                fallback = segment.toLowerCase(Locale.US);
            }
        }
        if (first != null) {
            return first;
        }
        return fallback != null ? fallback : "request";
    }

    private static boolean namedSegment(String segment) {
        String token = segment.toLowerCase(Locale.US);
        return !token.isEmpty() && token.indexOf('.') < 0 && !token.startsWith("rst_")
                && !token.matches("v\\d+") && !token.matches("\\d+")
                && !token.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private static String operationToken(String segment) {
        String token = segment.toLowerCase(Locale.US);
        if (token.isEmpty() || token.indexOf('.') >= 0 || token.startsWith("rst_")) {
            return null;
        }
        if (token.matches("v\\d+") || token.matches("\\d{4}-\\d{2}-\\d{2}") || token.matches("\\d+")) {
            return null;
        }
        if (token.startsWith("collage-ai") || token.startsWith("polishv") || token.contains("-ai-")) {
            return null;
        }
        for (String routing : ROUTING_SEGMENTS) {
            if (token.equals(routing)) {
                return null;
            }
        }
        if (token.startsWith(POLISH_PREFIX)) {
            token = token.substring(POLISH_PREFIX.length());
        }
        for (String suffix : new String[]{"_trt", "_v2", "_v3"}) {
            if (token.endsWith(suffix)) {
                token = token.substring(0, token.length() - suffix.length());
            }
        }
        return token.isEmpty() ? null : token;
    }

    static String labelOf(String operation) {
        for (String[] entry : LABELS) {
            if (operation.equals(entry[0]) || operation.startsWith(entry[0] + "_")) {
                return entry[1];
            }
        }
        StringBuilder label = new StringBuilder();
        for (String word : operation.split("[_-]")) {
            if (word.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.length() == 0 ? "AI request" : label.toString();
    }

    private static String resourceOf(String url) {
        if (url == null) {
            return null;
        }
        int start = url.indexOf("rst_");
        if (start < 0) {
            Matcher match = UUID.matcher(url);
            return match.find() ? match.group() : null;
        }
        int end = url.indexOf('?', start);
        String tail = end < 0 ? url.substring(start) : url.substring(start, end);
        int dot = tail.lastIndexOf('.');
        return dot > 0 ? tail.substring(0, dot) : tail;
    }

    static String path(String url) {
        if (url == null) {
            return "/";
        }
        int host = url.indexOf("://");
        int start = host < 0 ? 0 : url.indexOf('/', host + 3);
        if (start < 0) {
            return "/";
        }
        String path = url.substring(start);
        int query = path.indexOf('?');
        return query < 0 ? path : path.substring(0, query);
    }

    static final class LiveRow {
        final String method;
        final String path;
        int statusCode;
        int count = 1;
        private String status;

        LiveRow(String method, String path, String status, int statusCode) {
            this.method = method;
            this.path = path;
            this.status = status;
            this.statusCode = statusCode;
        }

        boolean unobserved() {
            return status == null;
        }

        boolean inFlight() {
            return status != null && status.isEmpty();
        }

        String status() {
            return status;
        }
    }

    public static final class Attempt {
        long startedAtMs;
        long endedAtMs;
        int statusCode = HttpStatus.NONE;
        String message = "";
        String protocol = "";
        String contentType = "";
        String body = "";
        long contentLength = -1;
        List<String[]> requestHeaders = new ArrayList<>();
        List<String[]> responseHeaders = new ArrayList<>();

        Attempt(long startedAtMs, long endedAtMs) {
            this.startedAtMs = startedAtMs;
            this.endedAtMs = endedAtMs;
        }

        public long durationMs() {
            long ended = endedAtMs == 0 ? System.currentTimeMillis() : endedAtMs;
            return Math.max(0, ended - startedAtMs);
        }

        public int statusCode() {
            return statusCode;
        }

        public String status() {
            return statusCode == HttpStatus.NONE ? "" : String.valueOf(statusCode);
        }

        public boolean inFlight() {
            return endedAtMs == 0;
        }

        public boolean dispatchOnly() {
            return statusCode == HttpStatus.NONE;
        }

        public String statusLine() {
            if (dispatchOnly()) {
                return message.isEmpty() ? "no response captured" : message;
            }
            String reason = message.isEmpty() ? HttpStatus.reason(statusCode) : message;
            return reason.isEmpty() ? status() : status() + " " + reason;
        }
    }

    public static final class Exchange {
        public final String method;
        public final String url;
        final List<Attempt> attempts = new ArrayList<>();
        List<String[]> payload = new ArrayList<>();

        Exchange(String method, String url) {
            this.method = method;
            this.url = url;
        }

        void add(Attempt attempt) {
            if (attempts.size() < MAX_ATTEMPTS_PER_EXCHANGE) {
                attempts.add(attempt);
            }
        }

        public String path() {
            return AiTrace.path(url);
        }

        public List<Attempt> attempts() {
            synchronized (LOCK) {
                return new ArrayList<>(attempts);
            }
        }

        public List<String[]> payload() {
            synchronized (LOCK) {
                return new ArrayList<>(payload);
            }
        }

        public boolean dispatchOnly() {
            synchronized (LOCK) {
                for (Attempt attempt : attempts) {
                    if (!attempt.dispatchOnly()) {
                        return false;
                    }
                }
                return !attempts.isEmpty();
            }
        }

        public Attempt last() {
            synchronized (LOCK) {
                return attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
            }
        }

        public long durationMs() {
            synchronized (LOCK) {
                Attempt last = last();
                return last == null ? 0 : Math.max(0, last.endedAtMs - attempts.get(0).startedAtMs);
            }
        }
    }

    public static final class Session {
        public final String operation;
        public final String feature;
        public final long startedAtMs;
        private final List<Exchange> exchanges = new ArrayList<>();
        boolean control;
        String resource;
        boolean postObserved = true;
        private long endedAtMs;

        Session(String operation, long startedAtMs) {
            this.operation = operation;
            this.feature = labelOf(operation);
            this.startedAtMs = startedAtMs;
            this.endedAtMs = startedAtMs;
        }

        Exchange exchange(String method, String url) {
            for (Exchange exchange : exchanges) {
                if (exchange.method.equals(method) && exchange.url.equals(url)) {
                    return exchange;
                }
            }
            Exchange exchange = new Exchange(method, url);
            exchanges.add(exchange);
            while (exchanges.size() > MAX_EXCHANGES_PER_SESSION) {
                exchanges.remove(0);
            }
            return exchange;
        }

        void touch(long atMs) {
            endedAtMs = Math.max(endedAtMs, atMs);
        }

        public List<Exchange> exchanges() {
            synchronized (LOCK) {
                return new ArrayList<>(exchanges);
            }
        }

        public boolean postObserved() {
            return postObserved;
        }

        public long endedAtMs() {
            return endedAtMs;
        }

        public long durationMs() {
            long ended = endedAtMs == 0 ? System.currentTimeMillis() : endedAtMs;
            return Math.max(0, ended - startedAtMs);
        }

        public int requestCount() {
            synchronized (LOCK) {
                int total = 0;
                for (Exchange exchange : exchanges) {
                    total += exchange.attempts.size();
                }
                return total;
            }
        }

        public int outcomeCode() {
            synchronized (LOCK) {
                for (int i = exchanges.size() - 1; i >= 0; i--) {
                    List<Attempt> attempts = exchanges.get(i).attempts;
                    for (int j = attempts.size() - 1; j >= 0; j--) {
                        if (!attempts.get(j).dispatchOnly()) {
                            return attempts.get(j).statusCode;
                        }
                    }
                }
                return HttpStatus.NONE;
            }
        }
    }
}

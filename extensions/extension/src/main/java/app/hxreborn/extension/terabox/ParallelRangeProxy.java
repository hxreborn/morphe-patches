/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.terabox;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ParallelRangeProxy {

    private static final String TAG = "TeraboxRangeProxy";
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final Pattern RANGE = Pattern.compile("bytes=(\\d{1,18})-(\\d{0,18})");
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes (\\d{1,18})-(\\d{1,18})/(\\d{1,18})");
    private static final byte[] NO_BODY = new byte[0];

    private static final int CONNECTIONS = 12;
    private static final int SEGMENT_SIZE = 512 * 1024;
    private static final int STARTUP_SEGMENT_SIZE = 128 * 1024;
    private static final int STARTUP_BYTES = 512 * 1024;
    private static final long READ_AHEAD_BYTES = 6L * 1024 * 1024;
    private static final long CACHE_BYTES = 24L * 1024 * 1024;
    private static final int REGISTERED_FILES = 2;
    private static final int FETCH_ATTEMPTS = 3;
    private static final int MAX_REDIRECTS = 5;
    private static final int TIMEOUT_MS = 15_000;
    private static final int CLIENT_TIMEOUT_MS = 30_000;
    private static final int MAX_REQUEST_HEAD_BYTES = 16 * 1024;
    private static final int READ_BUFFER = 16 * 1024;
    private static final int ERROR_BODY_LIMIT = 8 * 1024;

    private static ParallelRangeProxy instance;

    private final ServerSocket serverSocket;
    private final ExecutorService clientThreads = Executors.newCachedThreadPool();
    private final SecureRandom random = new SecureRandom();
    private final LinkedHashMap<String, ProxiedFile> proxiedFiles = new LinkedHashMap<>(16, 0.75f, true);

    private ParallelRangeProxy() throws IOException {
        serverSocket = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
    }

    static String proxyUrl(String originUrl) throws IOException {
        ParallelRangeProxy proxy = startedInstance();
        return "http://127.0.0.1:" + proxy.serverSocket.getLocalPort() + "/" + proxy.register(originUrl);
    }

    private static synchronized ParallelRangeProxy startedInstance() throws IOException {
        if (instance == null) {
            ParallelRangeProxy proxy = new ParallelRangeProxy();
            Thread acceptor = new Thread(proxy::acceptClients, "hx-terabox-proxy");
            acceptor.setDaemon(true);
            acceptor.start();
            instance = proxy;
        }
        return instance;
    }

    private String register(String originUrl) {
        synchronized (proxiedFiles) {
            for (Map.Entry<String, ProxiedFile> entry : proxiedFiles.entrySet()) {
                if (entry.getValue().originUrl.equals(originUrl)) {
                    return entry.getKey();
                }
            }
            String token = new BigInteger(128, random).toString(16);
            proxiedFiles.put(token, new ProxiedFile(originUrl));
            Iterator<ProxiedFile> leastRecent = proxiedFiles.values().iterator();
            while (proxiedFiles.size() > REGISTERED_FILES) {
                leastRecent.next().close();
                leastRecent.remove();
            }
            return token;
        }
    }

    private ProxiedFile proxiedFile(String token) {
        synchronized (proxiedFiles) {
            return proxiedFiles.get(token);
        }
    }

    private void acceptClients() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                clientThreads.execute(() -> serve(client));
            } catch (IOException exception) {
                Log.w(TAG, "Proxy accept failed", exception);
            }
        }
    }

    private void serve(Socket client) {
        try (Socket socket = client) {
            socket.setSoTimeout(CLIENT_TIMEOUT_MS);
            Request request = Request.read(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            if (request == null) {
                writeStatus(out, "400 Bad Request", "", NO_BODY);
                return;
            }
            if (!request.method.equals("GET") && !request.method.equals("HEAD")) {
                writeStatus(out, "405 Method Not Allowed", "", NO_BODY);
                return;
            }
            ProxiedFile file = proxiedFile(request.token);
            if (file == null) {
                writeStatus(out, "404 Not Found", "", NO_BODY);
                return;
            }
            Origin origin;
            try {
                origin = file.resolve(request);
            } catch (OriginRejected rejection) {
                writeStatus(out, rejection.status, "", rejection.body);
                return;
            }
            long start = request.rangeStart;
            long end = request.rangeEnd < 0 ? origin.length - 1 : Math.min(request.rangeEnd, origin.length - 1);
            if (start > end) {
                writeStatus(out, "416 Range Not Satisfiable", "Content-Range: bytes */" + origin.length + "\r\n", NO_BODY);
                return;
            }
            writeHeaders(out, request.hasRange, start, end, origin);
            if (request.method.equals("GET")) {
                file.stream(out, start, end);
            }
        } catch (IOException exception) {
            Log.d(TAG, "Proxy request failed: " + exception.getMessage());
        } catch (RuntimeException exception) {
            Log.w(TAG, "Proxy client failed", exception);
        }
    }

    private static void writeHeaders(OutputStream out, boolean partial, long start, long end, Origin origin)
            throws IOException {
        StringBuilder head = new StringBuilder(partial ? "HTTP/1.1 206 Partial Content\r\n" : "HTTP/1.1 200 OK\r\n");
        head.append("Content-Type: ").append(origin.contentType).append("\r\n");
        head.append("Accept-Ranges: bytes\r\n");
        head.append("Content-Length: ").append(end - start + 1).append("\r\n");
        if (partial) {
            head.append("Content-Range: bytes ").append(start).append('-').append(end).append('/')
                    .append(origin.length).append("\r\n");
        }
        head.append("Connection: close\r\n\r\n");
        out.write(head.toString().getBytes(ASCII));
    }

    private static void writeStatus(OutputStream out, String status, String headers, byte[] body) throws IOException {
        out.write(("HTTP/1.1 " + status + "\r\n" + headers + "Content-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n").getBytes(ASCII));
        out.write(body);
        out.flush();
    }

    private static final class Request {
        final String method;
        final String token;
        final String query;
        final boolean hasRange;
        final long rangeStart;
        final long rangeEnd;
        final String cookie;

        private Request(String method, String target, String range, String cookie) {
            int queryStart = target.indexOf('?');
            String path = queryStart < 0 ? target : target.substring(0, queryStart);
            this.method = method;
            this.token = path.substring(path.lastIndexOf('/') + 1);
            this.query = queryStart < 0 ? null : target.substring(queryStart + 1);
            this.cookie = cookie;
            Matcher matcher = range == null ? null : RANGE.matcher(range.trim());
            hasRange = matcher != null && matcher.matches();
            rangeStart = hasRange ? Long.parseLong(matcher.group(1)) : 0;
            rangeEnd = hasRange && matcher.group(2).length() != 0 ? Long.parseLong(matcher.group(2)) : -1;
        }

        static Request read(InputStream in) throws IOException {
            String head = readHead(new BufferedInputStream(in));
            if (head == null) {
                return null;
            }
            String[] lines = head.split("\r?\n");
            String[] parts = lines[0].split(" ");
            if (parts.length < 2) {
                return null;
            }
            String range = null;
            String cookie = null;
            for (int index = 1; index < lines.length; index++) {
                String line = lines[index];
                int colon = line.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                if (name.equals("range")) {
                    range = value;
                } else if (name.equals("cookie")) {
                    cookie = value;
                }
            }
            return new Request(parts[0], parts[1], range, cookie);
        }

        private static String readHead(InputStream in) throws IOException {
            StringBuilder head = new StringBuilder();
            int lineLength = 0;
            int next;
            while (head.length() < MAX_REQUEST_HEAD_BYTES && (next = in.read()) >= 0) {
                head.append((char) next);
                if (next == '\n') {
                    if (lineLength == 0) {
                        return head.toString().trim();
                    }
                    lineLength = 0;
                } else if (next != '\r') {
                    lineLength++;
                }
            }
            return null;
        }
    }

    private static final class OriginRejected extends IOException {
        final String status;
        final byte[] body;

        OriginRejected(int code, String message, byte[] body) {
            super("Origin returned " + code);
            this.status = code + " " + (message == null ? "Error" : message);
            this.body = body;
        }
    }

    private static final class Origin {
        final String url;
        final String cookie;
        final long length;
        final String contentType;

        Origin(String url, String cookie, long length, String contentType) {
            this.url = url;
            this.cookie = cookie;
            this.length = length;
            this.contentType = contentType;
        }
    }

    private static final class ProxiedFile {
        final String originUrl;
        private final ThreadPoolExecutor fetchers = new ThreadPoolExecutor(CONNECTIONS, CONNECTIONS,
                30, TimeUnit.SECONDS, new PriorityBlockingQueue<>());
        private final AtomicLong streams = new AtomicLong();
        private final AtomicLong submissions = new AtomicLong();
        private final Object resolveLock = new Object();
        private final TreeMap<Long, Segment> segments = new TreeMap<>();
        private Origin origin;
        private String cookie;
        private String query;

        ProxiedFile(String originUrl) {
            this.originUrl = originUrl;
            fetchers.allowCoreThreadTimeOut(true);
        }

        void close() {
            List<Segment> unfinished;
            synchronized (this) {
                unfinished = new ArrayList<>(segments.values());
                segments.clear();
            }
            for (Runnable queued : fetchers.shutdownNow()) {
                ((Fetch) queued).discard();
            }
            for (Segment segment : unfinished) {
                segment.fail(new IOException("Proxied file closed"));
            }
        }

        Origin resolve(Request request) throws IOException {
            synchronized (resolveLock) {
                synchronized (this) {
                    if (request.cookie != null) {
                        cookie = request.cookie;
                    }
                    if (request.query != null) {
                        query = request.query;
                    }
                    if (origin != null) {
                        return origin;
                    }
                }
                return resolveOrigin(request.rangeStart);
            }
        }

        private Origin refresh(Origin failed) throws IOException {
            synchronized (resolveLock) {
                synchronized (this) {
                    if (origin != failed) {
                        return origin;
                    }
                }
                return resolveOrigin(-1);
            }
        }

        private synchronized Origin currentOrigin() {
            return origin;
        }

        void stream(OutputStream out, long start, long end) throws IOException {
            long streamOrder = streams.incrementAndGet();
            long position = start;
            long scheduledUntil = start;
            while (position <= end) {
                long readAhead = position - start < STARTUP_BYTES
                        ? (long) CONNECTIONS * STARTUP_SEGMENT_SIZE
                        : READ_AHEAD_BYTES;
                long readAheadEnd = Math.min(end + 1, position + readAhead);
                while (scheduledUntil < readAheadEnd) {
                    scheduledUntil = segmentAt(scheduledUntil, start, end, streamOrder).end + 1;
                }
                Segment segment = segmentAt(position, start, end, streamOrder);
                long available = Math.min(end + 1, segment.awaitFilled(position));
                out.write(segment.bytes(), (int) (position - segment.start), (int) (available - position));
                position = available;
            }
            out.flush();
        }

        private Segment segmentAt(long position, long requestStart, long requestEnd, long streamOrder) {
            Segment segment;
            synchronized (this) {
                Map.Entry<Long, Segment> covering = segments.floorEntry(position);
                segment = covering == null ? null : covering.getValue();
                if (segment == null || segment.end < position || segment.failed()) {
                    long size = position - requestStart < STARTUP_BYTES ? STARTUP_SEGMENT_SIZE : SEGMENT_SIZE;
                    long end = Math.min(Math.min(requestEnd, origin.length - 1), position + size - 1);
                    Long following = segments.higherKey(position);
                    if (following != null) {
                        end = Math.min(end, following - 1);
                    }
                    segment = new Segment(position, end);
                    segments.put(position, segment);
                    evictCompleted();
                }
                segment.markUsed();
            }
            if (!segment.claimed()) {
                submit(new Fetch(this, segment, streamOrder, submissions.incrementAndGet(), null));
            }
            return segment;
        }

        private void submit(Fetch fetch) {
            try {
                fetchers.execute(fetch);
            } catch (RejectedExecutionException exception) {
                fetch.discard();
            }
        }

        private void evictCompleted() {
            long cached = 0;
            for (Segment segment : segments.values()) {
                cached += segment.length;
            }
            while (cached > CACHE_BYTES) {
                Segment leastRecent = null;
                for (Segment segment : segments.values()) {
                    if (segment.done() && (leastRecent == null || segment.lastUsed() < leastRecent.lastUsed())) {
                        leastRecent = segment;
                    }
                }
                if (leastRecent == null) {
                    return;
                }
                segments.remove(leastRecent.start);
                cached -= leastRecent.length;
            }
        }

        void download(Segment segment) throws IOException {
            IOException lastFailure = null;
            Origin current = currentOrigin();
            for (int attempt = 0; attempt < FETCH_ATTEMPTS; attempt++) {
                if (attempt > 0) {
                    current = refresh(current);
                }
                long from = segment.start + segment.filled();
                HttpURLConnection connection = open(current.url, current.cookie, from, segment.end);
                try {
                    int status = connection.getResponseCode();
                    if (status != HttpURLConnection.HTTP_PARTIAL) {
                        throw new IOException("Range " + from + "-" + segment.end + " returned " + status);
                    }
                    requireContentRange(connection, from, segment.end, current.length);
                    segment.fill(connection.getInputStream());
                    return;
                } catch (IOException exception) {
                    connection.disconnect();
                    lastFailure = exception;
                }
            }
            throw lastFailure;
        }

        private Origin resolveOrigin(long firstByte) throws IOException {
            String url;
            String requestCookie;
            synchronized (this) {
                url = query == null ? originUrl : originUrl + (originUrl.indexOf('?') < 0 ? '?' : '&') + query;
                requestCookie = cookie;
            }
            long start = Math.max(0, firstByte);
            long end = firstByte < 0 ? 0 : start + STARTUP_SEGMENT_SIZE - 1;
            for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
                HttpURLConnection connection = open(url, requestCookie, start, end);
                int status;
                try {
                    status = connection.getResponseCode();
                } catch (IOException exception) {
                    connection.disconnect();
                    throw exception;
                }
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect();
                    if (location == null) {
                        throw new IOException("Redirect " + status + " without Location");
                    }
                    URL redirectFrom = new URL(url);
                    URL redirectTo = new URL(redirectFrom, location);
                    if (!redirectTo.getHost().equals(redirectFrom.getHost())) {
                        requestCookie = null;
                    }
                    url = redirectTo.toString();
                    continue;
                }
                if (status != HttpURLConnection.HTTP_PARTIAL) {
                    OriginRejected rejection = status >= 400
                            ? new OriginRejected(status, connection.getResponseMessage(), errorBody(connection))
                            : new OriginRejected(HttpURLConnection.HTTP_BAD_GATEWAY, "Bad Gateway", NO_BODY);
                    connection.disconnect();
                    throw rejection;
                }
                long length;
                try {
                    length = contentRangeTotal(connection);
                    requireContentRange(connection, start, Math.min(end, length - 1), length);
                } catch (IOException exception) {
                    connection.disconnect();
                    throw exception;
                }
                String contentType = connection.getContentType();
                Origin resolved = new Origin(url, requestCookie, length,
                        contentType == null ? "application/octet-stream" : contentType);
                if (firstByte < 0) {
                    connection.disconnect();
                    synchronized (this) {
                        origin = resolved;
                    }
                    return resolved;
                }
                Segment first = new Segment(start, Math.min(end, length - 1));
                first.claim();
                synchronized (this) {
                    origin = resolved;
                    segments.put(start, first);
                }
                submit(new Fetch(this, first, Long.MAX_VALUE, submissions.incrementAndGet(), connection));
                return resolved;
            }
            throw new IOException("More than " + MAX_REDIRECTS + " redirects from " + originUrl);
        }
    }

    private static final class Fetch implements Runnable, Comparable<Fetch> {
        final ProxiedFile file;
        final Segment segment;
        private final long streamOrder;
        private final long submission;
        private final HttpURLConnection openResponse;

        Fetch(ProxiedFile file, Segment segment, long streamOrder, long submission, HttpURLConnection openResponse) {
            this.file = file;
            this.segment = segment;
            this.streamOrder = streamOrder;
            this.submission = submission;
            this.openResponse = openResponse;
        }

        @Override
        public int compareTo(Fetch other) {
            int newerStreamFirst = Long.compare(other.streamOrder, streamOrder);
            return newerStreamFirst != 0 ? newerStreamFirst : Long.compare(submission, other.submission);
        }

        void discard() {
            if (openResponse != null) {
                openResponse.disconnect();
            }
            segment.fail(new IOException("Proxied file closed"));
        }

        @Override
        public void run() {
            if (openResponse == null && !segment.claim()) {
                return;
            }
            try {
                if (openResponse != null) {
                    try {
                        segment.fill(openResponse.getInputStream());
                        return;
                    } catch (IOException exception) {
                        openResponse.disconnect();
                    }
                }
                file.download(segment);
            } catch (IOException exception) {
                segment.fail(exception);
            } catch (RuntimeException exception) {
                segment.fail(new IOException(exception));
            }
        }
    }

    private static final class Segment {
        final long start;
        final long end;
        final int length;
        private byte[] bytes;
        private int filled;
        private IOException failure;
        private boolean claimed;
        private long lastUsed;

        Segment(long start, long end) {
            this.start = start;
            this.end = end;
            this.length = (int) (end - start + 1);
        }

        synchronized boolean done() {
            return filled == length || failure != null;
        }

        synchronized boolean failed() {
            return failure != null;
        }

        synchronized int filled() {
            return filled;
        }

        synchronized boolean claimed() {
            return claimed;
        }

        synchronized long lastUsed() {
            return lastUsed;
        }

        synchronized void markUsed() {
            lastUsed = System.nanoTime();
        }

        synchronized boolean claim() {
            if (claimed) {
                return false;
            }
            claimed = true;
            bytes = new byte[length];
            return true;
        }

        synchronized byte[] bytes() {
            return bytes;
        }

        void fill(InputStream in) throws IOException {
            try (InputStream stream = in) {
                byte[] buffer = new byte[READ_BUFFER];
                int wanted;
                while ((wanted = Math.min(buffer.length, length - filled())) > 0) {
                    int count = stream.read(buffer, 0, wanted);
                    if (count < 0) {
                        throw new IOException("Range " + start + "-" + end + " ended after " + filled() + " bytes");
                    }
                    append(buffer, count);
                }
            }
        }

        private synchronized void append(byte[] buffer, int count) {
            System.arraycopy(buffer, 0, bytes, filled, count);
            filled += count;
            notifyAll();
        }

        synchronized void fail(IOException error) {
            if (filled < length) {
                failure = error;
                notifyAll();
            }
        }

        synchronized long awaitFilled(long position) throws IOException {
            while (start + filled <= position) {
                if (failure != null) {
                    throw failure;
                }
                try {
                    wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting for byte " + position, exception);
                }
            }
            return start + filled;
        }
    }

    private static long contentRangeTotal(HttpURLConnection connection) throws IOException {
        String header = connection.getHeaderField("Content-Range");
        Matcher range = CONTENT_RANGE.matcher(String.valueOf(header));
        if (!range.matches()) {
            throw new IOException("Origin sent Content-Range " + header);
        }
        return Long.parseLong(range.group(3));
    }

    private static void requireContentRange(HttpURLConnection connection, long start, long end, long length)
            throws IOException {
        String header = connection.getHeaderField("Content-Range");
        Matcher range = CONTENT_RANGE.matcher(String.valueOf(header));
        if (!range.matches() || Long.parseLong(range.group(1)) != start || Long.parseLong(range.group(2)) != end
                || Long.parseLong(range.group(3)) != length) {
            throw new IOException("Origin sent Content-Range " + header + " for bytes " + start + "-" + end + "/" + length);
        }
    }

    private static HttpURLConnection open(String url, String cookie, long start, long end) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
        connection.setRequestProperty("Accept-Encoding", "identity");
        if (cookie != null) {
            connection.setRequestProperty("Cookie", cookie);
        }
        return connection;
    }

    private static byte[] errorBody(HttpURLConnection connection) {
        InputStream error = connection.getErrorStream();
        if (error == null) {
            return NO_BODY;
        }
        try (InputStream in = error) {
            byte[] buffer = new byte[ERROR_BODY_LIMIT];
            int length = 0;
            int count;
            while (length < buffer.length && (count = in.read(buffer, length, buffer.length - length)) >= 0) {
                length += count;
            }
            return Arrays.copyOf(buffer, length);
        } catch (IOException exception) {
            Log.d(TAG, "Could not read origin error body: " + exception.getMessage());
            return NO_BODY;
        }
    }
}

/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DashServer implements Runnable {
    interface Source {
        DashFile open(String subjectId, int season, int episode, int height, String origin, long originSize)
                throws IOException;
    }

    static final class Unavailable extends IOException {
        private static final long serialVersionUID = 1L;

        Unavailable(String message) {
            super(message);
        }
    }

    private static final class Range {
        final long start;
        final long end;

        Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

    static final int PORT = 47291;
    private static final String TAG = "hxreborn/moviebox";
    private static final byte[] LOOPBACK = {127, 0, 0, 1};
    private static final int BACKLOG = 16;
    private static final int WORKERS = 8;
    private static final int QUEUED_REQUESTS = 16;
    private static final long WORKER_KEEP_ALIVE_S = 30L;
    private static final int SOCKET_TIMEOUT_MS = 30000;
    private static final int OUTPUT_BUFFER_BYTES = 1 << 16;
    private static final int MAX_HEAD_BYTES = 8192;
    private static final byte[] HEAD_END = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
    private static final String RANGE_HEADER = "Range:";
    private static final Pattern PATH = Pattern.compile("^/dash/(\\d{1,19})/(\\d{1,9})/(\\d{1,9})/(\\d{1,9})\\.mp4$");
    private static final Pattern RANGE = Pattern.compile("^bytes=(\\d{1,18})-(\\d{0,18})$");
    private static final char DEL = 0x7F;
    private static final String ORIGIN_PARAMETER = "origin";
    private static final String ORIGIN_SIZE_PARAMETER = "size";
    private static final String STATUS_OK = "200 OK";
    private static final String STATUS_PARTIAL = "206 Partial Content";
    private static final String STATUS_FOUND = "302 Found";
    private static final String STATUS_NOT_FOUND = "404 Not Found";
    private static final String STATUS_UNSATISFIABLE = "416 Range Not Satisfiable";
    private static final String STATUS_UNAVAILABLE = "503 Service Unavailable";

    private final Source source;
    private final ServerSocket serverSocket;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(WORKERS, WORKERS, WORKER_KEEP_ALIVE_S,
            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(QUEUED_REQUESTS));
    private boolean started;

    private DashServer(Source source, ServerSocket serverSocket) {
        this.source = source;
        this.serverSocket = serverSocket;
    }

    static DashServer bind(Source source) throws IOException {
        return new DashServer(source, new ServerSocket(PORT, BACKLOG, InetAddress.getByAddress(LOOPBACK)));
    }

    static String url(String subjectId, int season, int episode, int height, String origin, long originSize) {
        return "http://127.0.0.1:" + PORT + "/dash/" + subjectId + "/" + season + "/" + episode + "/" + height + ".mp4"
                + "?" + ORIGIN_PARAMETER + "=" + Uri.encode(origin) + "&" + ORIGIN_SIZE_PARAMETER + "=" + originSize;
    }

    synchronized void start() {
        if (started) throw new IllegalStateException("already started");
        started = true;
        Thread acceptor = new Thread(this, "hx-dash-server");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    void close() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        workers.shutdownNow();
    }

    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            final Socket client;
            try {
                client = serverSocket.accept();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) Log.e(TAG, "DASH server stopped", e);
                workers.shutdown();
                return;
            }
            try {
                workers.execute(new Runnable() {
                    @Override
                    public void run() {
                        serve(client);
                    }
                });
            } catch (RejectedExecutionException overloaded) {
                closeQuietly(client);
            }
        }
    }

    private void serve(Socket client) {
        try {
            client.setSoTimeout(SOCKET_TIMEOUT_MS);
            String[] requestHead = readHead(client.getInputStream()).split("\r\n");
            OutputStream out = new BufferedOutputStream(client.getOutputStream(), OUTPUT_BUFFER_BYTES);
            respond(requestHead[0], rangeHeader(requestHead), out);
            out.flush();
        } catch (IOException e) {
            Log.w(TAG, "DASH request failed", e);
        } finally {
            closeQuietly(client);
        }
    }

    private static String readHead(InputStream in) throws IOException {
        byte[] head = new byte[MAX_HEAD_BYTES];
        int length = 0;
        while (length < head.length) {
            int next = in.read();
            if (next < 0) break;
            head[length++] = (byte) next;
            if (length >= HEAD_END.length && endsWithHeadEnd(head, length)) {
                return new String(head, 0, length - HEAD_END.length, StandardCharsets.ISO_8859_1);
            }
        }
        throw new IOException("request head missing or over " + MAX_HEAD_BYTES + " bytes");
    }

    private static boolean endsWithHeadEnd(byte[] head, int length) {
        for (int i = 0; i < HEAD_END.length; i++) {
            if (head[length - HEAD_END.length + i] != HEAD_END[i]) return false;
        }
        return true;
    }

    private static String rangeHeader(String[] requestHead) {
        for (int i = 1; i < requestHead.length; i++) {
            if (requestHead[i].regionMatches(true, 0, RANGE_HEADER, 0, RANGE_HEADER.length())) {
                return requestHead[i].substring(RANGE_HEADER.length()).trim();
            }
        }
        return null;
    }

    private void respond(String requestLine, String rangeHeader, OutputStream out) throws IOException {
        String[] request = requestLine.split(" ");
        boolean headOnly = request.length >= 2 && "HEAD".equals(request[0]);
        Uri target = request.length >= 2 ? Uri.parse("http://127.0.0.1" + request[1]) : null;
        Matcher path = target == null || target.getPath() == null ? null : PATH.matcher(target.getPath());
        if (path == null || !path.matches() || !(headOnly || "GET".equals(request[0]))) {
            writeEmpty(out, STATUS_NOT_FOUND, null);
            return;
        }
        String origin = validOrigin(target.getQueryParameter(ORIGIN_PARAMETER));
        long originSize = parseSize(target.getQueryParameter(ORIGIN_SIZE_PARAMETER));

        DashFile file;
        try {
            file = source.open(path.group(1), Integer.parseInt(path.group(2)),
                    Integer.parseInt(path.group(3)), Integer.parseInt(path.group(4)), origin, originSize);
        } catch (Unavailable gone) {
            Log.i(TAG, "unavailable " + request[1] + ": " + gone.getMessage());
            writeEmpty(out, STATUS_NOT_FOUND, null);
            return;
        } catch (IOException e) {
            Log.e(TAG, "cannot open " + request[1], e);
            writeEmpty(out, STATUS_UNAVAILABLE, null);
            return;
        }
        if (file == null) {
            if (origin == null) {
                writeEmpty(out, STATUS_NOT_FOUND, null);
            } else {
                StringBuilder sb = header(STATUS_FOUND).append("Location: ").append(origin).append("\r\n")
                        .append("Content-Length: 0\r\n\r\n");
                out.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
            }
            return;
        }

        Matcher requested = rangeHeader == null ? null : RANGE.matcher(rangeHeader);
        boolean partial = requested != null && requested.matches();
        Range range = partial ? requestedRange(requested, file.length) : new Range(0, file.length - 1);
        if (range.start > range.end) {
            writeEmpty(out, STATUS_UNSATISFIABLE, "bytes */" + file.length);
            return;
        }
        writeHeaders(out, range, file.length, partial);
        if (!headOnly) file.write(out, range.start, range.end);
    }

    private static String validOrigin(String origin) {
        if (origin == null || !(origin.startsWith("http://") || origin.startsWith("https://"))) return null;
        for (int i = 0; i < origin.length(); i++) {
            char c = origin.charAt(i);
            if (c <= ' ' || c >= DEL) return null;
        }
        return origin;
    }

    private static long parseSize(String value) {
        if (value == null) return 0;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException malformed) {
            return 0;
        }
    }

    private static Range requestedRange(Matcher requested, long length) {
        long start = Long.parseLong(requested.group(1));
        String last = requested.group(2);
        long end = last.isEmpty() ? length - 1 : Math.min(length - 1, Long.parseLong(last));
        return new Range(start, end);
    }

    private static void writeHeaders(OutputStream out, Range range, long length, boolean partial) throws IOException {
        StringBuilder sb = header(partial ? STATUS_PARTIAL : STATUS_OK)
                .append("Content-Length: ").append(range.end - range.start + 1).append("\r\n");
        if (partial) {
            sb.append("Content-Range: bytes ").append(range.start).append('-').append(range.end)
                    .append('/').append(length).append("\r\n");
        }
        out.write(sb.append("\r\n").toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private static void writeEmpty(OutputStream out, String status, String contentRange) throws IOException {
        StringBuilder sb = header(status).append("Content-Length: 0\r\n");
        if (contentRange != null) sb.append("Content-Range: ").append(contentRange).append("\r\n");
        out.write(sb.append("\r\n").toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private static StringBuilder header(String status) {
        return new StringBuilder("HTTP/1.1 ").append(status).append("\r\n")
                .append("Connection: close\r\nAccept-Ranges: bytes\r\nContent-Type: video/mp4\r\n");
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}

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
package hx.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalCdn implements AutoCloseable {

    private static final Pattern RANGE = Pattern.compile("bytes=(\\d+)-(\\d+)");

    private final ServerSocket socket;

    private final ExecutorService workers = Executors.newCachedThreadPool();

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();

    private final Map<String, List<String>> ranges = new ConcurrentHashMap<>();

    private final AtomicInteger active = new AtomicInteger();

    private final AtomicInteger peakActive = new AtomicInteger();

    private final String requiredCookie;

    private volatile long rangeDelayMs;

    private volatile boolean rangesIgnored;

    public LocalCdn(String requiredCookie) throws IOException {
        this.requiredCookie = requiredCookie;
        this.socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        this.workers.execute(this::acceptLoop);
    }

    public void put(String path, byte[] body) {
        this.files.put(path, body);
    }

    public String url(String path) {
        return "http://127.0.0.1:" + this.socket.getLocalPort() + path;
    }

    public int hitsOn(String path) {
        AtomicInteger count = this.hits.get(path);
        return (count != null) ? count.get() : 0;
    }

    public void ignoreRanges() {
        this.rangesIgnored = true;
    }

    public void delayRanges(long delayMs) {
        this.rangeDelayMs = delayMs;
    }

    public List<String> rangesOn(String path) {
        List<String> served = this.ranges.get(path);
        return (served != null) ? new ArrayList<>(served) : Collections.<String>emptyList();
    }

    public int peakConcurrentRanges() {
        return this.peakActive.get();
    }

    public void forgetRanges() {
        this.ranges.clear();
        this.peakActive.set(0);
    }

    @Override
    public void close() {
        try {
            this.socket.close();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        this.workers.shutdownNow();
    }

    private void acceptLoop() {
        while (!this.socket.isClosed()) {
            try {
                Socket client = this.socket.accept();
                this.workers.execute(() -> serve(client));
            } catch (IOException ex) {
                return;
            }
        }
    }

    private void serve(Socket client) {
        try (Socket connection = client) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.ISO_8859_1));
            String requestLine = in.readLine();
            String range = null;
            String cookie = null;
            for (String line = in.readLine(); line != null && !line.isEmpty(); line = in.readLine()) {
                String lower = line.toLowerCase(java.util.Locale.ROOT);
                if (lower.startsWith("range:")) {
                    range = line.substring("range:".length()).trim();
                } else if (lower.startsWith("cookie:")) {
                    cookie = line.substring("cookie:".length()).trim();
                }
            }
            if (requestLine != null) {
                answer(connection.getOutputStream(), pathOf(requestLine), range, cookie);
            }
        } catch (IOException ex) {
            return;
        }
    }

    private void answer(OutputStream out, String path, String range, String cookie) throws IOException {
        this.hits.computeIfAbsent(path, (key) -> new AtomicInteger()).incrementAndGet();
        byte[] body = this.files.get(path);
        if (body == null) {
            respond(out, "404 Not Found", new byte[0], null);
        } else if (this.requiredCookie != null && !this.requiredCookie.equals(cookie)) {
            respond(out, "403 Forbidden", new byte[0], null);
        } else {
            answerWithRange(out, path, body, range);
        }
    }

    private void answerWithRange(OutputStream out, String path, byte[] body, String range) throws IOException {
        Matcher matcher = (range != null) ? RANGE.matcher(range) : null;
        if (this.rangesIgnored || matcher == null || !matcher.matches()) {
            respond(out, "200 OK", body, null);
            return;
        }
        int start = Integer.parseInt(matcher.group(1));
        int end = Math.min(Integer.parseInt(matcher.group(2)), body.length - 1);
        this.ranges.computeIfAbsent(path, (key) -> new CopyOnWriteArrayList<>()).add(start + "-" + end);
        int inFlight = this.active.incrementAndGet();
        this.peakActive.accumulateAndGet(inFlight, Math::max);
        try {
            pauseBeforeAnswering();
            respond(out, "206 Partial Content", Arrays.copyOfRange(body, start, end + 1),
                    "bytes " + start + "-" + end + "/" + body.length);
        } finally {
            this.active.decrementAndGet();
        }
    }

    private void pauseBeforeAnswering() {
        try {
            Thread.sleep(this.rangeDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String pathOf(String requestLine) {
        String target = requestLine.split(" ")[1];
        int query = target.indexOf('?');
        return (query >= 0) ? target.substring(0, query) : target;
    }

    private static void respond(OutputStream out, String status, byte[] body, String contentRange) throws IOException {
        StringBuilder head = new StringBuilder("HTTP/1.1 ").append(status).append("\r\n");
        head.append("Content-Length: ").append(body.length).append("\r\n");
        if (contentRange != null) {
            head.append("Content-Range: ").append(contentRange).append("\r\n");
        }
        head.append("Connection: close\r\n\r\n");
        out.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }

}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.terabox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ParallelRangeProxyTest {

    private static final int LENGTH = 2_621_440 + 12_345;

    private static final byte[] DATA = pattern(LENGTH);

    private static final int MAX_REQUEST_HEAD = 16 * 1024;

    private final List<FakeOrigin> origins = new ArrayList<>();

    @After
    public void closeOrigins() {
        for (FakeOrigin origin : this.origins) {
            origin.close();
        }
    }

    private static byte[] pattern(int length) {
        final byte[] bytes = new byte[length];
        new Random(7).nextBytes(bytes);
        return bytes;
    }

    private FakeOrigin origin(byte[] content, Responder responder) throws IOException {
        final FakeOrigin origin = new FakeOrigin(content, responder);
        this.origins.add(origin);
        return origin;
    }

    private FakeOrigin origin() throws IOException {
        return origin(DATA, FakeOrigin::serveRange);
    }

    private String proxy(FakeOrigin origin, String path) throws IOException {
        return ParallelRangeProxy.proxyUrl(origin.url(path));
    }

    private static Response get(String proxyUrl, String... headers) throws IOException {
        return send(proxyUrl, "GET", "", headers);
    }

    private static Response getRange(String proxyUrl, String range) throws IOException {
        return get(proxyUrl, "Range: bytes=" + range);
    }

    private static Response send(String proxyUrl, String method, String suffix, String... headers) throws IOException {
        final URL url = new URL(proxyUrl);
        final StringBuilder head = new StringBuilder(method).append(' ')
            .append(url.getPath())
            .append(suffix)
            .append(" HTTP/1.1\r\nHost: 127.0.0.1\r\n");
        for (String header : headers) {
            head.append(header).append("\r\n");
        }
        head.append("\r\n");
        return exchange(url, head.toString().getBytes(StandardCharsets.US_ASCII), false);
    }

    private static Response exchange(URL url, byte[] request, boolean shutdownOutput) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", url.getPort())) {
            socket.setSoTimeout(10_000);
            final OutputStream out = socket.getOutputStream();
            out.write(request);
            out.flush();
            if (shutdownOutput) {
                socket.shutdownOutput();
            }
            final ByteArrayOutputStream raw = new ByteArrayOutputStream();
            final byte[] buffer = new byte[64 * 1024];
            final InputStream in = socket.getInputStream();
            int count;
            while ((count = in.read(buffer)) >= 0) {
                raw.write(buffer, 0, count);
            }
            return Response.parse(raw.toByteArray());
        }
    }

    private static byte[] slice(byte[] content, long start, long end) {
        return Arrays.copyOfRange(content, (int) start, (int) end + 1);
    }

    private static String tokenOf(String proxyUrl) {
        return proxyUrl.substring(proxyUrl.lastIndexOf('/') + 1);
    }

    private static void assertNoSuccess(String message, Response response) {
        assertTrue(message + " returned " + response.status, response.status < 0 || response.status >= 400);
    }

    @Test
    public void servesWholeFileWhenNoRangeIsSent() throws IOException {
        // when
        final Response response = get(proxy(origin(), "/file"));

        // then
        assertEquals(200, response.status);
        assertEquals(String.valueOf(LENGTH), response.header("content-length"));
        assertEquals("bytes", response.header("accept-ranges"));
        assertEquals("video/mp4", response.header("content-type"));
        assertNull(response.header("content-range"));
        assertArrayEquals(DATA, response.body);
    }

    @Test
    public void servesBoundedRangeAsPartialContent() throws IOException {
        // when
        final Response response = getRange(proxy(origin(), "/file"), "100-199");

        // then
        assertEquals(206, response.status);
        assertEquals("100", response.header("content-length"));
        assertEquals("bytes 100-199/" + LENGTH, response.header("content-range"));
        assertArrayEquals(slice(DATA, 100, 199), response.body);
    }

    @Test
    public void servesOpenEndedRangeToTheEndOfTheFile() throws IOException {
        // given
        final long start = LENGTH - 1000;

        // when
        final Response response = getRange(proxy(origin(), "/file"), start + "-");

        // then
        assertEquals(206, response.status);
        assertEquals("1000", response.header("content-length"));
        assertEquals("bytes " + start + "-" + (LENGTH - 1) + "/" + LENGTH, response.header("content-range"));
        assertArrayEquals(slice(DATA, start, LENGTH - 1), response.body);
    }

    @Test
    public void clampsRangeEndPastTheEndOfTheFile() throws IOException {
        // given
        final long start = LENGTH - 10;

        // when
        final Response response = getRange(proxy(origin(), "/file"), start + "-999999999");

        // then
        assertEquals(206, response.status);
        assertEquals("bytes " + start + "-" + (LENGTH - 1) + "/" + LENGTH, response.header("content-range"));
        assertEquals("10", response.header("content-length"));
        assertArrayEquals(slice(DATA, start, LENGTH - 1), response.body);
    }

    @Test
    public void servesRangeStartingMidFileOnTheFirstRequest() throws IOException {
        // when
        final Response response = getRange(proxy(origin(), "/file"), "700001-2100000");

        // then
        assertEquals(206, response.status);
        assertArrayEquals(slice(DATA, 700_001, 2_100_000), response.body);
    }

    @Test
    public void servesSeeksInEitherDirectionOnOneFile() throws IOException {
        final String proxyUrl = proxy(origin(), "/file");
        final long[][] ranges = { { 2_000_000, LENGTH - 1 }, { 0, 9_999 }, { 1_000_000, 1_500_000 },
                { 131_071, 131_073 }, { 524_287, 524_289 } };
        for (long[] range : ranges) {
            final Response response = getRange(proxyUrl, range[0] + "-" + range[1]);
            assertEquals(206, response.status);
            assertArrayEquals(range[0] + "-" + range[1], slice(DATA, range[0], range[1]), response.body);
        }
    }

    @Test
    public void servesConcurrentRangesWithTheirOwnBytes() throws Exception {
        final String proxyUrl = proxy(origin(), "/file");
        final long[][] ranges = { { 0, 600_000 }, { 900_000, 1_700_000 }, { 2_000_000, LENGTH - 1 },
                { 300_000, 1_200_000 } };
        final ExecutorService clients = Executors.newFixedThreadPool(ranges.length);
        try {
            final List<Future<Response>> responses = new ArrayList<>();
            for (long[] range : ranges) {
                final Callable<Response> request = () -> getRange(proxyUrl, range[0] + "-" + range[1]);
                responses.add(clients.submit(request));
            }
            for (int i = 0; i < ranges.length; i++) {
                final Response response = responses.get(i).get(30, TimeUnit.SECONDS);
                assertEquals(206, response.status);
                assertArrayEquals(slice(DATA, ranges[i][0], ranges[i][1]), response.body);
            }
        } finally {
            clients.shutdownNow();
        }
    }

    @Test
    public void servesFilesSmallerThanOneSegment() throws IOException {
        final byte[] small = Arrays.copyOf(DATA, 1000);
        final String proxyUrl = proxy(origin(small, FakeOrigin::serveRange), "/small");
        final Response whole = get(proxyUrl);
        assertEquals(200, whole.status);
        assertArrayEquals(small, whole.body);
        final Response part = getRange(proxyUrl, "10-19");
        assertEquals("bytes 10-19/1000", part.header("content-range"));
        assertArrayEquals(slice(small, 10, 19), part.body);
    }

    @Test
    public void answersHeadWithoutBody() throws IOException {
        // when
        final Response response = send(proxy(origin(), "/file"), "HEAD", "");

        // then
        assertEquals(200, response.status);
        assertEquals(String.valueOf(LENGTH), response.header("content-length"));
        assertEquals(0, response.body.length);
    }

    @Test
    public void rejectsUnsupportedMethod() throws IOException {
        // when
        final Response response = send(proxy(origin(), "/file"), "POST", "");

        // then
        assertEquals(405, response.status);
        assertEquals(0, response.body.length);
    }

    @Test
    public void rejectsUnknownToken() throws IOException {
        // given
        final String proxyUrl = proxy(origin(), "/file");

        // when
        final Response response = get(proxyUrl.replace(tokenOf(proxyUrl), "0000"));

        // then
        assertEquals(404, response.status);
    }

    @Test
    public void readsTokenFromTheLastPathSegment() throws IOException {
        // given
        final String proxyUrl = proxy(origin(), "/file");

        // when
        final Response response = getRange(proxyUrl.replace(tokenOf(proxyUrl), "ignored/prefix/" + tokenOf(proxyUrl)),
                "0-9");

        // then
        assertEquals(206, response.status);
        assertArrayEquals(slice(DATA, 0, 9), response.body);
    }

    @Test
    public void rejectsMalformedRequestLine() throws IOException {
        // given
        final String proxyUrl = proxy(origin(), "/file");

        // when
        final Response response = exchange(new URL(proxyUrl), "GARBAGE\r\n\r\n".getBytes(StandardCharsets.US_ASCII),
                false);

        // then
        assertEquals(400, response.status);
    }

    @Test
    public void rejectsRequestEndingBeforeTheHeaderTerminator() throws IOException {
        // given
        final String proxyUrl = proxy(origin(), "/file");
        final byte[] request = ("GET " + new URL(proxyUrl).getPath() + " HTTP/1.1\r\nHost: a\r\n")
            .getBytes(StandardCharsets.US_ASCII);

        // when
        final Response response = exchange(new URL(proxyUrl), request, true);

        // then
        assertEquals(400, response.status);
    }

    @Test
    public void rejectsRequestHeadOverTheSizeLimit() throws IOException {
        // given
        final String proxyUrl = proxy(origin(), "/file");
        final byte[] request = paddedHead(new URL(proxyUrl).getPath(), MAX_REQUEST_HEAD, false, null);
        assertEquals(MAX_REQUEST_HEAD, request.length);

        // when
        final Response response = exchange(new URL(proxyUrl), request, false);

        // then
        assertEquals(400, response.status);
    }

    @Test
    public void acceptsRequestHeadJustUnderTheSizeLimit() throws IOException {
        // given
        final String proxyUrl = proxy(origin(), "/file");
        final byte[] request = paddedHead(new URL(proxyUrl).getPath(), MAX_REQUEST_HEAD - 100, true,
                "Range: bytes=0-9");

        // when
        final Response response = exchange(new URL(proxyUrl), request, false);

        // then
        assertEquals(206, response.status);
        assertArrayEquals(slice(DATA, 0, 9), response.body);
    }

    @Test
    public void parsesMixedCaseHeadersInARequestWithBareLineFeeds() throws IOException {
        // given
        final String proxyUrl = proxy(origin(), "/file");
        final byte[] request = ("GET " + new URL(proxyUrl).getPath() + " HTTP/1.1\nrAnGe: bytes=5-14\n\n")
            .getBytes(StandardCharsets.US_ASCII);

        // when
        final Response response = exchange(new URL(proxyUrl), request, false);

        // then
        assertEquals(206, response.status);
        assertEquals("bytes 5-14/" + LENGTH, response.header("content-range"));
        assertArrayEquals(slice(DATA, 5, 14), response.body);
    }

    @Test
    public void servesWholeFileForRangeFormsItDoesNotSupport() throws IOException {
        final String proxyUrl = proxy(origin(), "/file");
        for (String range : new String[] { "bytes=-500", "bytes=0-4,10-14", "items=0-4", "bytes=a-b" }) {
            final Response response = get(proxyUrl, "Range: " + range);
            assertEquals(range, 200, response.status);
            assertArrayEquals(range, DATA, response.body);
        }
    }

    @Test
    public void answersRangeBeyondTheFileWith416AfterTheFileIsKnown() throws IOException {
        final String proxyUrl = proxy(origin(), "/file");
        assertEquals(206, getRange(proxyUrl, "0-9").status);
        final Response response = getRange(proxyUrl, LENGTH + "-");
        assertEquals(416, response.status);
        assertEquals("bytes */" + LENGTH, response.header("content-range"));
        assertEquals(0, response.body.length);
        final Response lastByte = getRange(proxyUrl, (LENGTH - 1) + "-");
        assertEquals(206, lastByte.status);
        assertArrayEquals(slice(DATA, LENGTH - 1, LENGTH - 1), lastByte.body);
    }

    @Test
    public void forwardsOriginRejectionOnTheFirstRequest() throws IOException {
        // when
        final Response response = getRange(proxy(origin(), "/file"), (LENGTH + 5) + "-");

        // then
        assertEquals(416, response.status);
    }

    @Test
    public void forwardsTheOriginErrorResponse() throws IOException {
        // given
        final FakeOrigin forbidden = origin(DATA, (origin, request, out) -> origin.write(out, "403 Forbidden", "",
                "denied".getBytes(StandardCharsets.US_ASCII)));

        // when
        final Response response = get(proxy(forbidden, "/file"));

        // then
        assertEquals(403, response.status);
        assertEquals("denied", new String(response.body, StandardCharsets.US_ASCII));
    }

    @Test
    public void answersBadGatewayWhenOriginIgnoresRange() throws IOException {
        // given
        final FakeOrigin ignoring = origin(DATA, (origin, request, out) -> origin.write(out, "200 OK", "", DATA));

        // when
        final Response response = get(proxy(ignoring, "/file"));

        // then
        assertEquals(502, response.status);
    }

    @Test
    public void rejectsUnusableOriginResponses() throws IOException {
        final Map<String, Responder> broken = new HashMap<>();
        broken.put("redirect loop",
                (origin, request, out) -> origin.write(out, "302 Found", "Location: /again\r\n", new byte[0]));
        broken.put("redirect without location",
                (origin, request, out) -> origin.write(out, "302 Found", "", new byte[0]));
        broken.put("range of another window", (origin, request, out) -> origin.write(out, "206 Partial Content",
                "Content-Range: bytes 5-9/" + LENGTH + "\r\n", new byte[5]));
        broken.put("unknown total length", (origin, request, out) -> origin.write(out, "206 Partial Content",
                "Content-Range: bytes 0-99/*\r\n", new byte[100]));
        broken.put("missing content range",
                (origin, request, out) -> origin.write(out, "206 Partial Content", "", new byte[100]));
        for (Map.Entry<String, Responder> entry : broken.entrySet()) {
            assertNoSuccess(entry.getKey(), get(proxy(origin(DATA, entry.getValue()), "/file")));
        }
    }

    @Test
    public void fetchesFromTheFinalUrlAfterRedirects() throws IOException {
        // given
        final FakeOrigin redirecting = origin(DATA, (origin, request, out) -> {
            if (request.target.equals("/start")) {
                origin.write(out, "302 Found", "Location: /final?x=1\r\n", new byte[0]);
            } else {
                origin.serveRange(request, out);
            }
        });

        // when
        final Response response = get(proxy(redirecting, "/start"));

        // then
        assertEquals(200, response.status);
        assertArrayEquals(DATA, response.body);
        assertEquals(1, redirecting.count("/start"));
        assertTrue(redirecting.count("/final?x=1") > 1);
        assertEquals(redirecting.requests.size(), redirecting.count("/start") + redirecting.count("/final?x=1"));
    }

    @Test
    public void keepsCookieAcrossSameHostRedirects() throws IOException {
        // given
        final FakeOrigin redirecting = origin(DATA, redirectTo("/final"));

        // when
        final Response response = get(proxy(redirecting, "/start"), "Cookie: sid=1");

        // then
        assertEquals(200, response.status);
        assertArrayEquals(DATA, response.body);
        for (Seen seen : redirecting.requests) {
            assertEquals(seen.target, "sid=1", seen.header("cookie"));
        }
    }

    @Test
    public void dropsCookieWhenRedirectedToAnotherHost() throws IOException {
        // given
        final FakeOrigin redirecting = origin(DATA, (origin, request, out) -> {
            if (request.target.equals("/start")) {
                origin.write(out, "302 Found", "Location: http://localhost:" + origin.port() + "/final\r\n",
                        new byte[0]);
            } else {
                origin.serveRange(request, out);
            }
        });

        // when
        final Response response = get(proxy(redirecting, "/start"), "Cookie: sid=1");

        // then
        assertEquals(200, response.status);
        assertArrayEquals(DATA, response.body);
        assertEquals("sid=1", redirecting.first("/start").header("cookie"));
        assertTrue(redirecting.count("/final") > 1);
        for (Seen seen : redirecting.requests) {
            if (seen.target.equals("/final")) {
                assertNull(seen.header("cookie"));
            }
        }
    }

    @Test
    public void appendsClientQueryToTheOriginUrl() throws IOException {
        final FakeOrigin signed = origin();
        final Response withQuery = send(proxy(signed, "/file?sig=abc"), "GET", "?t=1", "Range: bytes=0-9");
        assertEquals(206, withQuery.status);
        assertEquals("/file?sig=abc&t=1", signed.requests.get(0).target);

        final FakeOrigin plain = origin();
        final Response onlyQuery = send(proxy(plain, "/plain"), "GET", "?t=2", "Range: bytes=0-9");
        assertEquals(206, onlyQuery.status);
        assertEquals("/plain?t=2", plain.requests.get(0).target);
    }

    @Test
    public void asksTheOriginForExplicitRangesWithoutCompression() throws IOException {
        // given
        final FakeOrigin origin = origin();

        // when
        final Response response = get(proxy(origin, "/file"));

        // then
        assertEquals(200, response.status);
        assertTrue(origin.requests.size() > 1);
        for (Seen seen : origin.requests) {
            assertNotNull(seen.header("range"));
            assertTrue(seen.header("range"), seen.header("range").matches("bytes=\\d+-\\d+"));
            assertEquals("identity", seen.header("accept-encoding"));
            assertEquals("GET", seen.method);
        }
    }

    @Test
    public void declaresOctetStreamWhenOriginSendsNoContentType() throws IOException {
        // given
        final FakeOrigin untyped = origin();
        untyped.contentType = null;

        // when
        final Response response = getRange(proxy(untyped, "/file"), "0-9");

        // then
        assertEquals("application/octet-stream", response.header("content-type"));
    }

    @Test
    public void registersEachOriginOnce() throws IOException {
        final FakeOrigin origin = origin();
        final String first = proxy(origin, "/file");
        assertEquals(first, proxy(origin, "/file"));
        assertTrue(first, first.startsWith("http://127.0.0.1:"));
        assertTrue(tokenOf(first), tokenOf(first).matches("[0-9a-f]+"));
        final String other = proxy(origin, "/other");
        assertNotEquals(tokenOf(first), tokenOf(other));
        assertEquals(new URL(first).getPort(), new URL(other).getPort());
    }

    @Test
    public void evictsTheLeastRecentlyUsedOriginBeyondTwo() throws IOException {
        final FakeOrigin origin = origin();
        final String first = proxy(origin, "/first");
        final String second = proxy(origin, "/second");
        assertEquals(206, getRange(first, "0-9").status);
        final String third = proxy(origin, "/third");
        assertEquals(404, getRange(second, "0-9").status);
        assertEquals(206, getRange(first, "0-9").status);
        assertEquals(206, getRange(third, "0-9").status);
    }

    @Test
    public void retriesASegmentAfterATransientOriginError() throws IOException {
        // given
        final AtomicBoolean failed = new AtomicBoolean();
        final FakeOrigin flaky = origin(DATA, (origin, request, out) -> {
            if (request.rangeStart() > 0 && failed.compareAndSet(false, true)) {
                origin.write(out, "500 Internal Server Error", "", new byte[0]);
            } else {
                origin.serveRange(request, out);
            }
        });

        // when
        final Response response = get(proxy(flaky, "/file"));

        // then
        assertTrue(failed.get());
        assertEquals(200, response.status);
        assertArrayEquals(DATA, response.body);
    }

    @Test
    public void resumesFromTheLastReceivedByteAfterATruncatedResponse() throws IOException {
        // given
        final int delivered = 1000;
        final AtomicBoolean truncated = new AtomicBoolean();
        final AtomicLong truncatedStart = new AtomicLong(-1);
        final FakeOrigin cutting = origin(DATA, (origin, request, out) -> {
            if (request.rangeStart() > 0 && truncated.compareAndSet(false, true)) {
                truncatedStart.set(request.rangeStart());
                origin.writeTruncated(out, request, delivered);
            } else {
                origin.serveRange(request, out);
            }
        });

        // when
        final Response response = get(proxy(cutting, "/file"));

        // then
        assertTrue(truncated.get());
        assertEquals(200, response.status);
        assertArrayEquals(DATA, response.body);
        boolean resumed = false;
        for (Seen seen : cutting.requests) {
            resumed |= seen.rangeStart() == truncatedStart.get() + delivered;
        }
        assertTrue("no request resumed at " + (truncatedStart.get() + delivered), resumed);
    }

    @Test
    public void truncatesTheBodyInsteadOfDeliveringWrongBytesWhenSegmentsKeepFailing() throws IOException {
        // given
        final FakeOrigin failing = origin(DATA, (origin, request, out) -> {
            if (request.rangeStart() > 0) {
                origin.write(out, "500 Internal Server Error", "", new byte[0]);
            } else {
                origin.serveRange(request, out);
            }
        });

        // when
        final Response response = get(proxy(failing, "/file"));

        // then
        assertEquals(200, response.status);
        assertEquals(String.valueOf(LENGTH), response.header("content-length"));
        assertTrue(response.body.length + " bytes", response.body.length > 0 && response.body.length < LENGTH);
        assertArrayEquals(slice(DATA, 0, response.body.length - 1), response.body);
    }

    private static Responder redirectTo(String location) {
        return (origin, request, out) -> {
            if (request.target.equals("/start")) {
                origin.write(out, "302 Found", "Location: " + location + "\r\n", new byte[0]);
            } else {
                origin.serveRange(request, out);
            }
        };
    }

    private static byte[] paddedHead(String path, int totalLength, boolean terminated, String extraHeader) {
        final String requestLine = "GET " + path + " HTTP/1.1\r\n";
        final String extra = (extraHeader != null) ? extraHeader + "\r\n" : "";
        final String terminator = (terminated) ? "\r\n" : "";
        final int paddingLine = totalLength - requestLine.length() - extra.length() - terminator.length();
        final StringBuilder padding = new StringBuilder("X-Pad: ");
        while (padding.length() < paddingLine - 2) {
            padding.append('a');
        }
        padding.append("\r\n");
        return (requestLine + extra + padding + terminator).getBytes(StandardCharsets.US_ASCII);
    }

    private interface Responder {

        void respond(FakeOrigin origin, Seen request, OutputStream out) throws IOException;

    }

    private static final class Seen {

        private static final Pattern RANGE = Pattern.compile("bytes=(\\d+)-(\\d*)");

        final String method;

        final String target;

        final Map<String, String> headers;

        Seen(String method, String target, Map<String, String> headers) {
            this.method = method;
            this.target = target;
            this.headers = headers;
        }

        String header(String name) {
            return this.headers.get(name);
        }

        long rangeStart() {
            final Matcher matcher = RANGE.matcher(String.valueOf(header("range")));
            return (matcher.matches()) ? Long.parseLong(matcher.group(1)) : -1;
        }

        long rangeEnd(long length) {
            final Matcher matcher = RANGE.matcher(String.valueOf(header("range")));
            if (!matcher.matches() || matcher.group(2).isEmpty()) {
                return length - 1;
            }
            return Math.min(Long.parseLong(matcher.group(2)), length - 1);
        }

    }

    private static final class FakeOrigin {

        final List<Seen> requests = new CopyOnWriteArrayList<>();

        volatile String contentType = "video/mp4";

        private final ServerSocket server;

        private final byte[] content;

        private final Responder responder;

        FakeOrigin(byte[] content, Responder responder) throws IOException {
            this.content = content;
            this.responder = responder;
            this.server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            final Thread acceptor = new Thread(this::acceptConnections, "fake-origin");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return this.server.getLocalPort();
        }

        String url(String path) {
            return "http://127.0.0.1:" + port() + path;
        }

        void close() {
            try {
                this.server.close();
            } catch (IOException ex) {
                throw new AssertionError(ex);
            }
        }

        int count(String target) {
            int count = 0;
            for (Seen seen : this.requests) {
                if (seen.target.equals(target)) {
                    count++;
                }
            }
            return count;
        }

        Seen first(String target) {
            for (Seen seen : this.requests) {
                if (seen.target.equals(target)) {
                    return seen;
                }
            }
            throw new AssertionError("origin never saw " + target);
        }

        void serveRange(Seen request, OutputStream out) throws IOException {
            final long start = request.rangeStart();
            if (start < 0) {
                write(out, "200 OK", "", this.content);
                return;
            }
            if (start >= this.content.length) {
                write(out, "416 Range Not Satisfiable", "Content-Range: bytes */" + this.content.length + "\r\n",
                        new byte[0]);
                return;
            }
            final long end = request.rangeEnd(this.content.length);
            write(out, "206 Partial Content",
                    "Content-Range: bytes " + start + "-" + end + "/" + this.content.length + "\r\n",
                    slice(this.content, start, end));
        }

        void writeTruncated(OutputStream out, Seen request, int deliveredBytes) throws IOException {
            final long start = request.rangeStart();
            final long end = request.rangeEnd(this.content.length);
            final byte[] full = slice(this.content, start, end);
            out.write(head("206 Partial Content",
                    "Content-Range: bytes " + start + "-" + end + "/" + this.content.length + "\r\n", full.length));
            out.write(full, 0, deliveredBytes);
            out.flush();
        }

        void write(OutputStream out, String status, String headers, byte[] body) throws IOException {
            out.write(head(status, headers, body.length));
            out.write(body);
            out.flush();
        }

        private byte[] head(String status, String headers, int bodyLength) {
            final String type = (this.contentType != null) ? "Content-Type: " + this.contentType + "\r\n" : "";
            return ("HTTP/1.1 " + status + "\r\n" + type + headers + "Content-Length: " + bodyLength
                    + "\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        }

        private void acceptConnections() {
            while (true) {
                final Socket socket;
                try {
                    socket = this.server.accept();
                } catch (IOException ex) {
                    return;
                }
                final Thread connection = new Thread(() -> handle(socket), "fake-origin-connection");
                connection.setDaemon(true);
                connection.start();
            }
        }

        private void handle(Socket socket) {
            try (Socket connection = socket) {
                connection.setSoTimeout(10_000);
                final Seen request = readRequest(connection.getInputStream());
                this.requests.add(request);
                this.responder.respond(this, request, connection.getOutputStream());
            } catch (IOException ex) {
                return;
            }
        }

        private static Seen readRequest(InputStream in) throws IOException {
            final StringBuilder head = new StringBuilder();
            while (!head.toString().endsWith("\r\n\r\n")) {
                final int next = in.read();
                if (next < 0) {
                    throw new IOException("closed before the request ended");
                }
                head.append((char) next);
            }
            final String[] lines = head.toString().split("\r\n");
            final String[] requestLine = lines[0].split(" ");
            final Map<String, String> headers = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                final int colon = lines[i].indexOf(':');
                if (colon > 0) {
                    headers.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT),
                            lines[i].substring(colon + 1).trim());
                }
            }
            return new Seen(requestLine[0], requestLine[1], headers);
        }

    }

    private static final class Response {

        final int status;

        final Map<String, String> headers;

        final byte[] body;

        private Response(int status, Map<String, String> headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        static Response parse(byte[] raw) {
            if (raw.length == 0) {
                return new Response(-1, new HashMap<>(), new byte[0]);
            }
            int headEnd = -1;
            for (int i = 0; i + 3 < raw.length && headEnd < 0; i++) {
                if (raw[i] == '\r' && raw[i + 1] == '\n' && raw[i + 2] == '\r' && raw[i + 3] == '\n') {
                    headEnd = i;
                }
            }
            if (headEnd < 0) {
                throw new AssertionError("response head never ended: " + new String(raw, StandardCharsets.ISO_8859_1));
            }
            final String[] lines = new String(raw, 0, headEnd, StandardCharsets.ISO_8859_1).split("\r\n");
            final Map<String, String> headers = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                final int colon = lines[i].indexOf(':');
                headers.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT),
                        lines[i].substring(colon + 1).trim());
            }
            return new Response(Integer.parseInt(lines[0].split(" ")[1]), headers,
                    Arrays.copyOfRange(raw, headEnd + 4, raw.length));
        }

        String header(String name) {
            return this.headers.get(name);
        }

    }

}

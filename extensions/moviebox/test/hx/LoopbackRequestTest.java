/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class LoopbackRequestTest {

    private static final String ORIGIN = "https://origin.example.com/f.mp4";

    @Test
    public void readsTheEpisodeFromTheBuiltUrl() {
        // given
        String url = DashServer.buildUrl("7700000000000000001", 2, 5, 1080, ORIGIN, 4096, "res-1");

        // when
        LoopbackRequest request = LoopbackRequest.parse(get(url.substring("http://127.0.0.1:47291".length())));

        // then
        assertEquals("7700000000000000001", request.subjectId);
        assertEquals(2, request.season);
        assertEquals(5, request.episode);
        assertEquals(1080, request.height);
        assertEquals(ORIGIN, request.origin);
        assertEquals(4096, request.originSize);
        assertEquals("res-1", request.resourceId);
    }

    @Test
    public void leavesTheResourceIdOutWhenTheUrlHasNone() {
        // given
        String url = DashServer.buildUrl("77", 0, 0, 720, ORIGIN, 0, null);

        // when
        LoopbackRequest request = LoopbackRequest.parse(get(url.substring("http://127.0.0.1:47291".length())));

        // then
        assertNull(request.resourceId);
    }

    @Test
    public void readsTheRangeHeaderCaseInsensitively() {
        // when
        LoopbackRequest request = LoopbackRequest
            .parse("GET /dash/77/0/0/720.mp4 HTTP/1.1\r\nhost: x\r\n" + "RANGE: bytes=100-\r\nAccept: */*");

        // then
        assertEquals(100, request.range.first);
        assertTrue(request.isPartial());
        assertFalse(request.startsAtBeginning());
    }

    @Test
    public void startsAtTheBeginningWithoutARange() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /dash/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        assertTrue(request.startsAtBeginning());
        assertFalse(request.isPartial());
        assertEquals(0, request.rangeWithin(50).start);
        assertEquals(49, request.rangeWithin(50).end);
    }

    @Test
    public void startsAtTheBeginningForARangeFromZero() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /dash/77/0/0/720.mp4 HTTP/1.1\r\nRange: bytes=0-9");

        // then
        assertTrue(request.startsAtBeginning());
        assertTrue(request.isPartial());
    }

    @Test
    public void answersAHeadRequestWithoutABody() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("HEAD /dash/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        assertTrue(request.headOnly);
    }

    @Test
    public void refusesOtherMethods() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("POST /dash/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        assertNull(request);
    }

    @Test
    public void refusesOtherPaths() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /other/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        assertNull(request);
    }

    @Test
    public void refusesARequestLineWithoutATarget() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET");

        // then
        assertNull(request);
    }

    @Test
    public void dropsAnOriginOutsideHttp() {
        // when
        LoopbackRequest request = LoopbackRequest
            .parse("GET /dash/77/0/0/720.mp4?origin=file%3A%2F%2F%2Fetc%2Fpasswd " + "HTTP/1.1\r\nHost: x");

        // then
        assertNull(request.origin);
    }

    @Test
    public void dropsAnOriginWithControlCharacters() {
        // when
        LoopbackRequest request = LoopbackRequest
            .parse("GET /dash/77/0/0/720.mp4?origin=https%3A%2F%2Fa.example%0Ab " + "HTTP/1.1\r\nHost: x");

        // then
        assertNull(request.origin);
    }

    @Test
    public void treatsAMalformedSizeAsUnknown() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /dash/77/0/0/720.mp4?size=big HTTP/1.1\r\nHost: x");

        // then
        assertEquals(0, request.originSize);
    }

    @Test
    public void readsTheHeadUpToTheBlankLine() throws IOException {
        // given
        byte[] bytes = "GET / HTTP/1.1\r\nHost: x\r\n\r\nbody".getBytes(StandardCharsets.ISO_8859_1);

        // when
        String head = LoopbackRequest.readHead(new ByteArrayInputStream(bytes));

        // then
        assertEquals("GET / HTTP/1.1\r\nHost: x", head);
    }

    @Test
    public void refusesAHeadThatNeverEnds() {
        // given
        byte[] bytes = new byte[9000];

        // when
        try {
            LoopbackRequest.readHead(new ByteArrayInputStream(bytes));
            fail("expected the head to be refused");
        } catch (IOException ex) {

            // then
            assertTrue(ex.getMessage(), ex.getMessage().contains("8192"));
        }
    }

    private static String get(String target) {
        return "GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1";
    }

}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public final class LoopbackResponseTest {

    @Test
    public void announcesAPartialResponseWithItsContentRange() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeHeaders(out, new ByteRange(10, 19), 100, true);

        // then
        String head = text(out);
        assertTrue(head, head.startsWith("HTTP/1.1 206 Partial Content\r\n"));
        assertTrue(head, head.contains("Content-Length: 10\r\n"));
        assertTrue(head, head.contains("Content-Range: bytes 10-19/100\r\n"));
        assertTrue(head, head.endsWith("\r\n\r\n"));
    }

    @Test
    public void announcesAWholeResponseWithoutAContentRange() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeHeaders(out, new ByteRange(0, 99), 100, false);

        // then
        String head = text(out);
        assertTrue(head, head.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(head, head.contains("Content-Length: 100\r\n"));
        assertFalse(head, head.contains("Content-Range"));
    }

    @Test
    public void advertisesRangesAndTheVideoType() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeHeaders(out, new ByteRange(0, 0), 1, false);

        // then
        String head = text(out);
        assertTrue(head, head.contains("Accept-Ranges: bytes\r\n"));
        assertTrue(head, head.contains("Content-Type: video/mp4\r\n"));
        assertTrue(head, head.contains("Connection: close\r\n"));
    }

    @Test
    public void redirectsToTheLocation() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeRedirect(out, "https://cdn.example.com/f.mp4");

        // then
        String head = text(out);
        assertTrue(head, head.startsWith("HTTP/1.1 302 Found\r\n"));
        assertTrue(head, head.contains("Location: https://cdn.example.com/f.mp4\r\n"));
        assertTrue(head, head.contains("Content-Length: 0\r\n"));
    }

    @Test
    public void reportsTheLengthOfAnUnsatisfiableRange() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeUnsatisfiable(out, 100);

        // then
        String head = text(out);
        assertTrue(head, head.startsWith("HTTP/1.1 416 Range Not Satisfiable\r\n"));
        assertTrue(head, head.contains("Content-Range: bytes */100\r\n"));
    }

    @Test
    public void answersNotFoundWithAnEmptyBody() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeNotFound(out);

        // then
        assertEquals(true, text(out).startsWith("HTTP/1.1 404 Not Found\r\n"));
        assertTrue(text(out).contains("Content-Length: 0\r\n"));
    }

    @Test
    public void answersUnavailableWithAnEmptyBody() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeUnavailable(out);

        // then
        assertTrue(text(out).startsWith("HTTP/1.1 503 Service Unavailable\r\n"));
    }

    private static String text(ByteArrayOutputStream out) {
        return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
    }

}

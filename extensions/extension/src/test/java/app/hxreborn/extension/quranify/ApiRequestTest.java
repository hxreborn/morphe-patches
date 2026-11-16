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
package app.hxreborn.extension.quranify;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public final class ApiRequestTest {

    private static final String BODY = "{\"surah\":1}";

    private static ApiRequest read(String raw) throws IOException {
        return ApiRequest.read(new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void splitsTargetIntoPathAndQuery() throws IOException {
        ApiRequest request = read("GET /api/v2/reciters?lang=en&page=2 HTTP/1.1\r\n\r\n");

        assertEquals("GET", request.method);
        assertEquals("/api/v2/reciters", request.path);
        assertEquals("?lang=en&page=2", request.query);
    }

    @Test
    public void keepsEmptyQueryWithoutQuestionMark() throws IOException {
        ApiRequest request = read("GET /api/v2/reciters HTTP/1.1\r\n\r\n");

        assertEquals("/api/v2/reciters", request.path);
        assertEquals("", request.query);
    }

    @Test
    public void forwardsOnlyAllowedHeadersInLowercase() throws IOException {
        ApiRequest request = read("GET /api/ HTTP/1.1\r\nX-Device-Id: device\r\nCookie: secret\r\n"
                + "Accept:  application/json \r\n\r\n");

        assertEquals("device", request.headers.get("x-device-id"));
        assertEquals("application/json", request.headers.get("accept"));
        assertFalse(request.headers.containsKey("cookie"));
    }

    @Test
    public void readsBodyOfDeclaredLength() throws IOException {
        ApiRequest request = read("POST /api/v2/sync HTTP/1.1\r\nContent-Length: " + BODY.length() + "\r\n\r\n" + BODY
                + "trailing");

        assertArrayEquals(BODY.getBytes(StandardCharsets.US_ASCII), request.body);
    }

    @Test
    public void acceptsLinesEndingInBareNewline() throws IOException {
        ApiRequest request = read("GET /api/v2/reciters HTTP/1.1\nAccept: */*\n\n");

        assertEquals("*/*", request.headers.get("accept"));
    }

    @Test
    public void rejectsBodyAboveLimit() {
        assertThrows(IOException.class, () -> read("POST /api/ HTTP/1.1\r\nContent-Length: 2000000\r\n\r\n"));
    }

    @Test
    public void rejectsNegativeContentLength() {
        assertThrows(IOException.class, () -> read("POST /api/ HTTP/1.1\r\nContent-Length: -1\r\n\r\n"));
    }

    @Test
    public void rejectsTruncatedBody() {
        assertThrows(IOException.class, () -> read("POST /api/ HTTP/1.1\r\nContent-Length: 10\r\n\r\nshort"));
    }

    @Test
    public void rejectsRequestLineWithoutVersion() {
        assertThrows(IOException.class, () -> read("GET /api/\r\n\r\n"));
    }

    @Test
    public void rejectsHeaderWithoutSeparator() {
        assertThrows(IOException.class, () -> read("GET /api/ HTTP/1.1\r\nbroken\r\n\r\n"));
    }

    @Test
    public void rejectsHeadersCutOffBeforeBlankLine() {
        assertThrows(IOException.class, () -> read("GET /api/ HTTP/1.1\r\nAccept: */*\r\n"));
    }

}

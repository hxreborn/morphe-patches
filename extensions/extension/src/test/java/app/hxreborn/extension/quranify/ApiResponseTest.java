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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public final class ApiResponseTest {

    private static final byte[] BODY = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);

    private static String write(Map<String, String> headers) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ApiResponse(200, "application/json", BODY, headers).writeTo(output);
        return output.toString(StandardCharsets.UTF_8.name());
    }

    @Test
    public void writesStatusHeadersAndBody() throws IOException {
        String written = write(Collections.singletonMap("etag", "\"v1\""));

        assertEquals("HTTP/1.1 200 Result\r\nContent-Type: application/json\r\nContent-Length: " + BODY.length
                + "\r\nConnection: close\r\netag: \"v1\"\r\n\r\n{\"ok\":true}", written);
    }

    @Test
    public void dropsHeaderValueContainingLineBreak() throws IOException {
        String written = write(Collections.singletonMap("location", "https://quranify.pro\r\nset-cookie: x"));

        assertEquals("HTTP/1.1 200 Result\r\nContent-Type: application/json\r\nContent-Length: " + BODY.length
                + "\r\nConnection: close\r\n\r\n{\"ok\":true}", written);
    }

}

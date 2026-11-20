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
package hx.loopback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(head.startsWith("HTTP/1.1 206 Partial Content\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Content-Length: 10\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Content-Range: bytes 10-19/100\r\n")).as(head).isTrue();
        Assertions.assertThat(head.endsWith("\r\n\r\n")).as(head).isTrue();
    }

    @Test
    public void announcesAWholeResponseWithoutAContentRange() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeHeaders(out, new ByteRange(0, 99), 100, false);

        // then
        String head = text(out);
        Assertions.assertThat(head.startsWith("HTTP/1.1 200 OK\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Content-Length: 100\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Content-Range")).as(head).isFalse();
    }

    @Test
    public void advertisesRangesAndTheVideoType() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeHeaders(out, new ByteRange(0, 0), 1, false);

        // then
        String head = text(out);
        Assertions.assertThat(head.contains("Accept-Ranges: bytes\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Content-Type: video/mp4\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Connection: close\r\n")).as(head).isTrue();
    }

    @Test
    public void redirectsToTheLocation() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeRedirect(out, "https://cdn.example.com/f.mp4");

        // then
        String head = text(out);
        Assertions.assertThat(head.startsWith("HTTP/1.1 302 Found\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Location: https://cdn.example.com/f.mp4\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Content-Length: 0\r\n")).as(head).isTrue();
    }

    @Test
    public void reportsTheLengthOfAnUnsatisfiableRange() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeUnsatisfiable(out, 100);

        // then
        String head = text(out);
        Assertions.assertThat(head.startsWith("HTTP/1.1 416 Range Not Satisfiable\r\n")).as(head).isTrue();
        Assertions.assertThat(head.contains("Content-Range: bytes */100\r\n")).as(head).isTrue();
    }

    @Test
    public void answersNotFoundWithAnEmptyBody() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeNotFound(out);

        // then
        Assertions.assertThat(text(out).startsWith("HTTP/1.1 404 Not Found\r\n")).isEqualTo(true);
        Assertions.assertThat(text(out).contains("Content-Length: 0\r\n")).isTrue();
    }

    @Test
    public void answersUnavailableWithAnEmptyBody() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        LoopbackResponse.writeUnavailable(out);

        // then
        Assertions.assertThat(text(out).startsWith("HTTP/1.1 503 Service Unavailable\r\n")).isTrue();
    }

    private static String text(ByteArrayOutputStream out) {
        return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
    }

}

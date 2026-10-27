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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(request.subjectId).isEqualTo("7700000000000000001");
        Assertions.assertThat(request.season).isEqualTo(2);
        Assertions.assertThat(request.episode).isEqualTo(5);
        Assertions.assertThat(request.height).isEqualTo(1080);
        Assertions.assertThat(request.origin).isEqualTo(ORIGIN);
        Assertions.assertThat(request.originSize).isEqualTo(4096);
        Assertions.assertThat(request.resourceId).isEqualTo("res-1");
    }

    @Test
    public void leavesTheResourceIdOutWhenTheUrlHasNone() {
        // given
        String url = DashServer.buildUrl("77", 0, 0, 720, ORIGIN, 0, null);

        // when
        LoopbackRequest request = LoopbackRequest.parse(get(url.substring("http://127.0.0.1:47291".length())));

        // then
        Assertions.assertThat(request.resourceId).isNull();
    }

    @Test
    public void readsTheRangeHeaderCaseInsensitively() {
        // when
        LoopbackRequest request = LoopbackRequest
            .parse("GET /dash/77/0/0/720.mp4 HTTP/1.1\r\nhost: x\r\n" + "RANGE: bytes=100-\r\nAccept: */*");

        // then
        Assertions.assertThat(request.range.first).isEqualTo(100);
        Assertions.assertThat(request.isPartial()).isTrue();
        Assertions.assertThat(request.startsAtBeginning()).isFalse();
    }

    @Test
    public void startsAtTheBeginningWithoutARange() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /dash/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        Assertions.assertThat(request.startsAtBeginning()).isTrue();
        Assertions.assertThat(request.isPartial()).isFalse();
        Assertions.assertThat(request.rangeWithin(50).start).isEqualTo(0);
        Assertions.assertThat(request.rangeWithin(50).end).isEqualTo(49);
    }

    @Test
    public void startsAtTheBeginningForARangeFromZero() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /dash/77/0/0/720.mp4 HTTP/1.1\r\nRange: bytes=0-9");

        // then
        Assertions.assertThat(request.startsAtBeginning()).isTrue();
        Assertions.assertThat(request.isPartial()).isTrue();
    }

    @Test
    public void marksAHeadRequestAsHeadOnly() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("HEAD /dash/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        Assertions.assertThat(request.headOnly).isTrue();
    }

    @Test
    public void refusesOtherMethods() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("POST /dash/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        Assertions.assertThat(request).isNull();
    }

    @Test
    public void refusesOtherPaths() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /other/77/0/0/720.mp4 HTTP/1.1\r\nHost: x");

        // then
        Assertions.assertThat(request).isNull();
    }

    @Test
    public void refusesARequestLineWithoutATarget() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET");

        // then
        Assertions.assertThat(request).isNull();
    }

    @Test
    public void dropsAnOriginOutsideHttp() {
        // when
        LoopbackRequest request = LoopbackRequest
            .parse("GET /dash/77/0/0/720.mp4?origin=file%3A%2F%2F%2Fetc%2Fpasswd " + "HTTP/1.1\r\nHost: x");

        // then
        Assertions.assertThat(request.origin).isNull();
    }

    @Test
    public void dropsAnOriginWithControlCharacters() {
        // when
        LoopbackRequest request = LoopbackRequest
            .parse("GET /dash/77/0/0/720.mp4?origin=https%3A%2F%2Fa.example%0Ab " + "HTTP/1.1\r\nHost: x");

        // then
        Assertions.assertThat(request.origin).isNull();
    }

    @Test
    public void treatsAMalformedSizeAsUnknown() {
        // when
        LoopbackRequest request = LoopbackRequest.parse("GET /dash/77/0/0/720.mp4?size=big HTTP/1.1\r\nHost: x");

        // then
        Assertions.assertThat(request.originSize).isEqualTo(0);
    }

    @Test
    public void readsTheHeadUpToTheBlankLine() throws IOException {
        // given
        byte[] bytes = "GET / HTTP/1.1\r\nHost: x\r\n\r\nbody".getBytes(StandardCharsets.ISO_8859_1);

        // when
        String head = LoopbackRequest.readHead(new ByteArrayInputStream(bytes));

        // then
        Assertions.assertThat(head).isEqualTo("GET / HTTP/1.1\r\nHost: x");
    }

    @Test
    public void refusesAHeadThatNeverEnds() {
        // given
        byte[] bytes = new byte[9000];

        // when
        Throwable failure = Assertions.catchThrowable(() -> LoopbackRequest.readHead(new ByteArrayInputStream(bytes)));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage().contains("8192")).as(failure.getMessage()).isTrue();
    }

    private static String get(String target) {
        return "GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1";
    }

}

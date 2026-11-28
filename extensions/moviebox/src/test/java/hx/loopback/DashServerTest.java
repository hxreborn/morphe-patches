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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import hx.dash.CdnCookies;
import hx.dash.CdnFile;
import hx.dash.DashFile;
import hx.testing.DashFixture;
import hx.testing.Mp4Fixtures;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class DashServerTest {

    private static final String TARGET = "/dash/77/1/2/720.mp4";

    private static final String CDN_FILE_NAME = "chunk-stream0-00001.m4s";

    private static final byte[] CDN_FILE = Mp4Fixtures.bytes(100_000, 5);

    private static final byte[] HEAD_END = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);

    private DashFixture fixture;

    private DashFile file;

    private DashServer server;

    @Before
    public void startServer() throws IOException {
        this.fixture = new DashFixture();
        this.file = this.fixture.open();
        this.fixture.cdn.put("/" + CDN_FILE_NAME, CDN_FILE);
        this.server = DashServer.bind(new FixtureSource());
        this.server.start();
    }

    @After
    public void stopServer() {
        this.server.close();
        this.fixture.close();
    }

    @Test
    public void answersAHeadRequestWithoutABody() throws IOException {
        // when
        byte[] response = send("HEAD");

        // then
        Assertions.assertThat(head(response).startsWith("HTTP/1.1 200 OK")).isTrue();
        Assertions.assertThat(head(response).contains("Content-Length: " + this.file.length))
            .as(head(response))
            .isTrue();
        Assertions.assertThat(bodyLength(response)).isEqualTo(0);
    }

    @Test
    public void answersAGetRequestWithTheWholeFile() throws IOException {
        // when
        byte[] response = send("GET");

        // then
        Assertions.assertThat(bodyLength(response)).isEqualTo(this.file.length);
    }

    @Test
    public void servesAFileWhoseLengthIsTheSizeTheAppExpects() throws IOException {
        // when
        byte[] response = send("GET", TARGET + "?size=" + this.file.length);

        // then
        Assertions.assertThat(bodyLength(response)).isEqualTo(this.file.length);
    }

    @Test
    public void refusesAFileWhoseLengthIsNotTheSizeTheAppExpects() throws IOException {
        // when
        byte[] response = send("GET", TARGET + "?size=" + (this.file.length - 1));

        // then
        Assertions.assertThat(head(response).startsWith("HTTP/1.1 404")).as(head(response)).isTrue();
    }

    @Test
    public void servesACdnFile() throws IOException {
        // when
        byte[] response = send("GET", cdnTarget());

        // then
        Assertions.assertThat(head(response).startsWith("HTTP/1.1 200 OK")).as(head(response)).isTrue();
        Assertions.assertThat(body(response)).isEqualTo(CDN_FILE);
    }

    @Test
    public void servesARangeOfACdnFile() throws IOException {
        // when
        byte[] response = send("GET", cdnTarget(), "Range: bytes=60000-\r\n");

        // then
        Assertions.assertThat(head(response).startsWith("HTTP/1.1 206")).as(head(response)).isTrue();
        Assertions.assertThat(head(response).contains("Content-Range: bytes 60000-99999/100000"))
            .as(head(response))
            .isTrue();
        Assertions.assertThat(body(response)).isEqualTo(Arrays.copyOfRange(CDN_FILE, 60_000, 100_000));
    }

    private static String cdnTarget() {
        return DashServer.buildCdnUrl("77", 0, 0, CDN_FILE_NAME).substring("http://127.0.0.1:47291".length());
    }

    private byte[] send(String method) throws IOException {
        return send(method, TARGET);
    }

    private byte[] send(String method, String target) throws IOException {
        return send(method, target, "");
    }

    private byte[] send(String method, String target, String headers) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), DashServer.PORT)) {
            OutputStream out = socket.getOutputStream();
            out.write((method + " " + target + " HTTP/1.1\r\nHost: x\r\n" + headers + "Connection: close\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            return readAll(socket.getInputStream());
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int count = in.read(buffer); count >= 0; count = in.read(buffer)) {
            all.write(buffer, 0, count);
        }
        return all.toByteArray();
    }

    private static int headEnd(byte[] response) {
        for (int i = 0; i + HEAD_END.length <= response.length; i++) {
            if (response[i] == HEAD_END[0] && response[i + 1] == HEAD_END[1] && response[i + 2] == HEAD_END[2]
                    && response[i + 3] == HEAD_END[3]) {
                return i + HEAD_END.length;
            }
        }
        throw new IllegalStateException("no end of head in " + response.length + " bytes");
    }

    private static String head(byte[] response) {
        return new String(response, 0, headEnd(response), StandardCharsets.ISO_8859_1);
    }

    private static int bodyLength(byte[] response) {
        return response.length - headEnd(response);
    }

    private static byte[] body(byte[] response) {
        return Arrays.copyOfRange(response, headEnd(response), response.length);
    }

    private final class FixtureSource implements DashFileSource {

        @Override
        public DashFile open(String subjectId, int season, int episode, int height, String origin, long originSize,
                String resourceId, boolean fromStart) {
            return DashServerTest.this.file;
        }

        @Override
        public CdnFile openCdnFile(String subjectId, int season, int episode, String fileName, long start)
                throws IOException {
            return CdnFile.open(DashServerTest.this.fixture.cdn.url("/" + fileName), start, new CdnCookies() {
                @Override
                public String current() {
                    return "fresh=1";
                }

                @Override
                public String refresh(String rejected) {
                    return "fresh=1";
                }
            });
        }

    }

}

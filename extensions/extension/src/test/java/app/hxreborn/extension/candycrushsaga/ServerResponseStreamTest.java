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
package app.hxreborn.extension.candycrushsaga;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ServerResponseStreamTest {

    private static final String METHOD = "CandyPopupConfigApi.getPopupUpdatedConfig";

    private static final byte[] REQUEST = ("[{\"id\":1,\"method\":\"" + METHOD + "\",\"params\":[]}]")
        .getBytes(StandardCharsets.UTF_8);

    private static final String CONFIG = "{\"configuration_states\":[{\"name\":\"default\"}]}";

    private static final int BUFFER_BYTES = 8192;

    private static final int JSON_NODE_LIMIT = 100000;

    private static final int OVERSIZED_RESPONSE_BYTES = 8 * 1024 * 1024 + 1;

    private static final int INVALID_UTF8_FIRST = 0xc3;

    private static final int INVALID_UTF8_SECOND = 0x28;

    @Test
    public void editGzippedTargetedReply() throws IOException {
        byte[] original = gzip(reply());
        InputStream edited = ServerResponses.response(new ByteArrayInputStream(original), gzip(REQUEST),
                Collections.singleton(METHOD), Collections.emptySet());
        byte[] bytes = read(edited);
        Assert.assertFalse(java.util.Arrays.equals(original, bytes));
        String decoded = new String(read(new GZIPInputStream(new ByteArrayInputStream(bytes))), StandardCharsets.UTF_8);
        Assert.assertTrue(decoded.contains("dynamic_offer"));
    }

    @Test
    public void preserveUntargetedStreamIdentity() throws IOException {
        InputStream source = new ByteArrayInputStream(reply());
        Assert.assertSame(source,
                ServerResponses.response(source, REQUEST, Collections.emptySet(), Collections.emptySet()));
    }

    @Test
    public void preserveMalformedUtf8Bytes() throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        response.write(reply(), 0, reply().length - 1);
        response.write(", {\"id\":2,\"result\":\"".getBytes(StandardCharsets.UTF_8));
        response.write(INVALID_UTF8_FIRST);
        response.write(INVALID_UTF8_SECOND);
        response.write("\"}]".getBytes(StandardCharsets.UTF_8));
        byte[] original = response.toByteArray();
        Assert.assertArrayEquals(original, read(ServerResponses.response(new ByteArrayInputStream(original), REQUEST,
                Collections.singleton(METHOD), Collections.emptySet())));
    }

    @Test
    public void preserveOversizedResponseBytes() throws IOException {
        byte[] original = new byte[OVERSIZED_RESPONSE_BYTES];
        java.util.Arrays.fill(original, (byte) 'x');
        original[original.length - 1] = 'z';
        Assert.assertArrayEquals(original, read(ServerResponses.response(new ByteArrayInputStream(original), REQUEST,
                Collections.singleton(METHOD), Collections.emptySet())));
    }

    @Test
    public void preserveBytesWhenTargetResultExceedsNodeLimit() throws IOException {
        StringBuilder values = new StringBuilder("[");
        for (int index = 0; index < JSON_NODE_LIMIT; index++) {
            if (index > 0) {
                values.append(',');
            }
            values.append('0');
        }
        values.append(']');
        byte[] original = ("[{\"id\":1,\"result\":{\"values\":" + values + ",\"configJsonString\":"
                + JSONObject.quote(CONFIG) + "}}]")
            .getBytes(StandardCharsets.UTF_8);
        Assert.assertArrayEquals(original, read(ServerResponses.response(new ByteArrayInputStream(original), REQUEST,
                Collections.singleton(METHOD), Collections.emptySet())));
    }

    @Test
    public void closeSourceAfterReadFailure() {
        FailingInputStream source = new FailingInputStream();
        try {
            ServerResponses.response(source, REQUEST, Collections.singleton(METHOD), Collections.emptySet());
            Assert.fail("Read failure expected");
        } catch (IOException ex) {
            Assert.assertTrue(source.closed);
        }
    }

    @Test
    public void stopReadingInterruptedRequests() throws IOException {
        FailingInputStream source = new FailingInputStream();
        Thread.currentThread().interrupt();
        try {
            ServerResponses.response(source, REQUEST, Collections.singleton(METHOD), Collections.emptySet());
            Assert.fail("Interruption expected");
        } catch (InterruptedIOException ex) {
            Assert.assertTrue(source.closed);
            Assert.assertFalse(source.read);
        } finally {
            Thread.interrupted();
        }
    }

    private static byte[] reply() {
        return ("[{\"id\":1,\"result\":{\"configJsonString\":" + JSONObject.quote(CONFIG) + "}}]")
            .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] gzip(byte[] bytes) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(compressed)) {
            output.write(bytes);
        }
        return compressed.toByteArray();
    }

    private static byte[] read(InputStream input) throws IOException {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_BYTES];
            int count;
            while ((count = source.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static final class FailingInputStream extends InputStream {

        private boolean closed;

        private boolean read;

        @Override
        public int read() throws IOException {
            this.read = true;
            throw new IOException("Read failed");
        }

        @Override
        public void close() {
            this.closed = true;
        }

    }

}

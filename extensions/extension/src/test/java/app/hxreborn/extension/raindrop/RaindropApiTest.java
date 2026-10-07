/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import android.webkit.CookieManager;
import org.json.JSONException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class RaindropApiTest {

    private static final String PATH = "hx-api-test/resource";

    private static byte[] sequence(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i * 31 + 7);
        }
        return bytes;
    }

    @BeforeClass
    public static void installFakeApi() {
        FakeRaindropApi.install();
    }

    @Before
    public void resetFakeApi() {
        FakeRaindropApi.reset();
    }

    @Test
    public void readsEmptyStream() throws IOException {
        // given
        FakeRaindropApi.respond(PATH, 200, new byte[0]);

        // when
        byte[] received = RaindropApi.getBytes(PATH);

        // then
        assertEquals(0, received.length);
    }

    @Test
    public void readsShortStream() throws IOException {
        // given
        byte[] bytes = sequence(10);
        FakeRaindropApi.respond(PATH, 200, bytes);

        // when
        byte[] received = RaindropApi.getBytes(PATH);

        // then
        assertArrayEquals(bytes, received);
    }

    @Test
    public void readsStreamsAroundTheBufferSize() throws IOException {
        for (int length : new int[] { 16383, 16384, 16385, 32768, 100000 }) {
            byte[] bytes = sequence(length);
            FakeRaindropApi.respond(PATH, 200, bytes);
            assertArrayEquals("length " + length, bytes, RaindropApi.getBytes(PATH));
        }
    }

    @Test
    public void joinsShortReads() throws IOException {
        // given
        byte[] bytes = sequence(300);
        FakeRaindropApi.respondOneByteAtATime(PATH, bytes);

        // when
        byte[] received = RaindropApi.getBytes(PATH);

        // then
        assertArrayEquals(bytes, received);
    }

    @Test
    public void parsesJsonBodies() throws IOException, JSONException {
        FakeRaindropApi.respond(PATH, "{\"result\":true,\"name\":\"café\"}");
        assertEquals("café", RaindropApi.get(PATH).getString("name"));
        FakeRaindropApi.respond(PATH, 200, "{\"name\":\"café\"}".getBytes(StandardCharsets.UTF_8));
        assertEquals("café", RaindropApi.get(PATH).getString("name"));
    }

    @Test(expected = JSONException.class)
    public void rejectsBodiesThatAreNotJsonObjects() throws IOException, JSONException {
        // given
        FakeRaindropApi.respond(PATH, "<html></html>");

        // when
        RaindropApi.get(PATH);
    }

    @Test
    public void failsOnStatusesOtherThan200() {
        for (int status : new int[] { 201, 204, 301, 401, 404, 500 }) {
            FakeRaindropApi.respond(PATH, status, "{}".getBytes(StandardCharsets.UTF_8));
            try {
                RaindropApi.getBytes(PATH);
                fail("expected an IOException for " + status);
            } catch (IOException ex) {
                assertEquals("GET " + PATH + " returned HTTP " + status, ex.getMessage());
            }
        }
    }

    @Test
    public void sendsTheSessionCookies() throws IOException {
        FakeRaindropApi.respond(PATH, "{}");
        RaindropApi.getBytes(PATH);
        assertNull(FakeRaindropApi.lastCookie());
        CookieManager.getInstance().setCookie(RaindropApi.API_URL, "session=abc123");
        RaindropApi.getBytes(PATH);
        assertEquals("session=abc123", FakeRaindropApi.lastCookie());
    }

}

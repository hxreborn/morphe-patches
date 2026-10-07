/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.notesnook;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class ProUnlockTest {

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static JSONObject rewrite(String json) throws JSONException {
        return new JSONObject(new String(ProUnlock.rewriteSubscription(bytes(json)), StandardCharsets.UTF_8));
    }

    private static byte[] paddedTo(int length) {
        final String prefix = "{\"subscription\":{\"plan\":0,\"status\":1},\"pad\":\"";
        final String suffix = "\"}";
        final byte[] body = new byte[length];
        Arrays.fill(body, (byte) 'x');
        System.arraycopy(bytes(prefix), 0, body, 0, prefix.length());
        System.arraycopy(bytes(suffix), 0, body, length - suffix.length(), suffix.length());
        return body;
    }

    @Test
    public void passesNullThrough() {
        assertNull(ProUnlock.rewriteSubscription(null));
    }

    @Test
    public void passesAnEmptyBodyThrough() {
        // given
        final byte[] empty = new byte[0];

        // when
        final byte[] result = ProUnlock.rewriteSubscription(empty);

        // then
        assertSame(empty, result);
    }

    @Test
    public void setsTheTopPlanWithAnActiveStatus() throws JSONException {
        // when
        final JSONObject subscription = rewrite("{\"id\":\"u1\",\"subscription\":{\"plan\":0,\"status\":1}}")
            .getJSONObject("subscription");

        // then
        assertEquals(3, subscription.getInt("plan"));
        assertEquals(0, subscription.getInt("status"));
    }

    @Test
    public void addsTheMissingPlanFieldsWhenTheSubscriptionHasNone() throws JSONException {
        // when
        final JSONObject subscription = rewrite("{\"subscription\":{}}").getJSONObject("subscription");

        // then
        assertEquals(3, subscription.getInt("plan"));
        assertEquals(0, subscription.getInt("status"));
    }

    @Test
    public void keepsEveryOtherField() throws JSONException {
        // when
        final JSONObject user = rewrite("{\"id\":\"u1\",\"email\":\"a@x\",\"verified\":true,"
                + "\"subscription\":{\"plan\":0,\"status\":1,\"provider\":2,\"expiry\":99}}");

        // then
        assertEquals("u1", user.getString("id"));
        assertEquals("a@x", user.getString("email"));
        assertTrue(user.getBoolean("verified"));
        final JSONObject subscription = user.getJSONObject("subscription");
        assertEquals(2, subscription.getInt("provider"));
        assertEquals(99, subscription.getInt("expiry"));
    }

    @Test
    public void keepsNonAsciiTextIntact() throws JSONException {
        // when
        final JSONObject user = rewrite("{\"name\":\"é中文\",\"subscription\":{\"plan\":0}}");

        // then
        assertEquals("é中文", user.getString("name"));
        assertEquals(3, user.getJSONObject("subscription").getInt("plan"));
    }

    @Test
    public void leavesBodiesWithoutASubscriptionKeyUntouched() {
        // given
        final byte[] body = bytes("{\"id\":\"u1\"}");

        // when
        final byte[] result = ProUnlock.rewriteSubscription(body);

        // then
        assertSame(body, result);
    }

    @Test
    public void leavesBodiesWhoseSubscriptionIsNotAnObjectUntouched() {
        for (String json : new String[] { "{\"subscription\":null}", "{\"subscription\":\"none\"}",
                "{\"subscription\":[1]}", "{\"subscription\":3}" }) {
            final byte[] body = bytes(json);
            assertSame(json, body, ProUnlock.rewriteSubscription(body));
        }
    }

    @Test
    public void leavesANestedSubscriptionUntouched() {
        // given
        final byte[] body = bytes("{\"user\":{\"subscription\":{\"plan\":0}}}");

        // when
        final byte[] result = ProUnlock.rewriteSubscription(body);

        // then
        assertSame(body, result);
    }

    @Test
    public void leavesBodiesThatAreNotJsonUntouched() {
        for (String text : new String[] { "\"subscription\" is mentioned here", "{\"subscription\":{\"plan\":0",
                "<html>\"subscription\"</html>" }) {
            final byte[] body = bytes(text);
            assertSame(text, body, ProUnlock.rewriteSubscription(body));
        }
    }

    @Test
    public void rewritesABodyAtTheSizeLimit() throws JSONException {
        // given
        final byte[] body = paddedTo(MAX_BODY_BYTES);
        assertEquals(MAX_BODY_BYTES, body.length);

        // when
        final byte[] rewritten = ProUnlock.rewriteSubscription(body);

        // then
        final JSONObject user = new JSONObject(new String(rewritten, StandardCharsets.UTF_8));
        assertEquals(3, user.getJSONObject("subscription").getInt("plan"));
    }

    @Test
    public void leavesABodyOverTheSizeLimitUntouched() {
        // given
        final byte[] body = paddedTo(MAX_BODY_BYTES + 1);
        final byte[] original = body.clone();

        // when
        final byte[] result = ProUnlock.rewriteSubscription(body);

        // then
        assertSame(body, result);
        assertArrayEquals(original, body);
    }

}

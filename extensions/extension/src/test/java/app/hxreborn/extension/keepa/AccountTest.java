/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class AccountTest {

    private static final long NOW = 1_000_000_000_000L;

    private static Account account(AccountState state, int tracked, int limit, long throttledUntil) {
        final Account account = new Account("a");
        account.state = state;
        account.tracked = tracked;
        account.limit = limit;
        account.throttledUntil = throttledUntil;
        return account;
    }

    @Test
    public void startsANewAccountWithDefaults() {
        // when
        final Account account = new Account("a");

        // then
        assertEquals("a", account.id);
        assertEquals("", account.token);
        assertEquals("", account.username);
        assertEquals("", account.email);
        assertEquals(AccountState.OK, account.state);
        assertEquals(0, account.throttledUntil);
        assertEquals(0, account.tracked);
        assertEquals(0, account.limit);
        assertEquals(0, account.refreshedAt);
        assertEquals(0, account.addedAt);
    }

    @Test
    public void readsEveryField() throws JSONException {
        // when
        final Account account = Account.fromJson(new JSONObject().put("id", "a")
            .put("token", "t")
            .put("username", "alice")
            .put("email", "a@x")
            .put("state", "throttled")
            .put("throttledUntil", NOW)
            .put("tracked", 12)
            .put("limit", 400)
            .put("refreshedAt", NOW + 1)
            .put("addedAt", NOW + 2));

        // then
        assertEquals("a", account.id);
        assertEquals("t", account.token);
        assertEquals("alice", account.username);
        assertEquals("a@x", account.email);
        assertEquals(AccountState.THROTTLED, account.state);
        assertEquals(NOW, account.throttledUntil);
        assertEquals(12, account.tracked);
        assertEquals(400, account.limit);
        assertEquals(NOW + 1, account.refreshedAt);
        assertEquals(NOW + 2, account.addedAt);
    }

    @Test
    public void fillsDefaultsForMissingFields() throws JSONException {
        // when
        final Account account = Account.fromJson(new JSONObject().put("id", "a"));

        // then
        assertEquals("", account.token);
        assertEquals("", account.username);
        assertEquals("", account.email);
        assertEquals(AccountState.OK, account.state);
        assertEquals(0, account.throttledUntil);
        assertEquals(0, account.tracked);
        assertEquals(0, account.limit);
        assertEquals(0, account.refreshedAt);
        assertEquals(0, account.addedAt);
    }

    @Test
    public void readsAnUnknownStateAsInvalid() throws JSONException {
        // when
        final Account account = Account.fromJson(new JSONObject().put("id", "a").put("state", "banned"));

        // then
        assertEquals(AccountState.INVALID, account.state);
    }

    @Test
    public void rejectsAnEntryWithoutAnId() {
        try {
            Account.fromJson(new JSONObject().put("token", "t"));
            throw new AssertionError("expected a failure");
        } catch (JSONException expected) {
            assertTrue(expected.getMessage().contains("id"));
        }
    }

    @Test
    public void writesEveryFieldUnderTheNamesItReadsThemFrom() throws JSONException {
        // given
        final Account account = new Account("a");
        account.token = "t";
        account.username = "alice";
        account.email = "a@x";
        account.state = AccountState.INVALID;
        account.throttledUntil = NOW;
        account.tracked = 3;
        account.limit = 200;
        account.refreshedAt = NOW + 1;
        account.addedAt = NOW + 2;

        // when
        final JSONObject json = account.toJson();

        // then
        assertEquals("a", json.getString("id"));
        assertEquals("t", json.getString("token"));
        assertEquals("alice", json.getString("username"));
        assertEquals("a@x", json.getString("email"));
        assertEquals("invalid", json.getString("state"));
        assertEquals(NOW, json.getLong("throttledUntil"));
        assertEquals(3, json.getInt("tracked"));
        assertEquals(200, json.getInt("limit"));
        assertEquals(NOW + 1, json.getLong("refreshedAt"));
        assertEquals(NOW + 2, json.getLong("addedAt"));
    }

    @Test
    public void roundTripsThroughJson() throws JSONException {
        // given
        final Account original = account(AccountState.THROTTLED, 7, 300, NOW);
        original.token = "t";
        original.username = "alice";
        original.email = "a@x";
        original.refreshedAt = 5;
        original.addedAt = 9;

        // when
        final Account copy = Account.fromJson(new JSONObject(original.toJson().toString()));

        // then
        assertEquals(original.id, copy.id);
        assertEquals(original.token, copy.token);
        assertEquals(original.username, copy.username);
        assertEquals(original.email, copy.email);
        assertEquals(original.state, copy.state);
        assertEquals(original.throttledUntil, copy.throttledUntil);
        assertEquals(original.tracked, copy.tracked);
        assertEquals(original.limit, copy.limit);
        assertEquals(original.refreshedAt, copy.refreshedAt);
        assertEquals(original.addedAt, copy.addedAt);
    }

    @Test
    public void keepsFieldsItDoesNotKnowWhenWritingBack() throws JSONException {
        // given
        final Account account = Account.fromJson(new JSONObject().put("id", "a").put("futureField", "kept"));
        account.tracked = 4;

        // when
        final JSONObject json = account.toJson();

        // then
        assertEquals("kept", json.getString("futureField"));
        assertEquals(4, json.getInt("tracked"));
    }

    @Test
    public void writesBackIntoTheJsonItWasReadFrom() throws JSONException {
        // given
        final JSONObject source = new JSONObject().put("id", "a");
        final Account account = Account.fromJson(source);
        account.username = "alice";

        // when
        final JSONObject written = account.toJson();

        // then
        assertSame(source, written);
        assertEquals("alice", source.getString("username"));
    }

    @Test
    public void marksInvalidOrThrottledAccountsHealthy() {
        final Account invalid = account(AccountState.INVALID, 0, 0, 0);
        invalid.markHealthy();
        assertEquals(AccountState.OK, invalid.state);

        final Account throttled = account(AccountState.THROTTLED, 0, 0, NOW + 60_000);
        throttled.markHealthy();
        assertEquals(AccountState.OK, throttled.state);
        assertEquals(0, throttled.throttledUntil);
    }

    @Test
    public void isThrottledOnlyWhileTheThrottleIsInTheFuture() {
        final Account account = account(AccountState.THROTTLED, 0, 0, NOW);
        assertTrue(account.isThrottled(NOW - 1));
        assertFalse(account.isThrottled(NOW));
        assertFalse(account.isThrottled(NOW + 1));
    }

    @Test
    public void onlyAThrottledStateCanBeThrottled() {
        assertFalse(account(AccountState.OK, 0, 0, NOW + 60_000).isThrottled(NOW));
        assertFalse(account(AccountState.INVALID, 0, 0, NOW + 60_000).isThrottled(NOW));
    }

    @Test
    public void cannotTrackWhenInvalid() {
        assertFalse(account(AccountState.INVALID, 0, 0, 0).canTrack(NOW));
        assertFalse(account(AccountState.INVALID, 0, 200, 0).canTrack(NOW));
    }

    @Test
    public void cannotTrackWhileThrottledButCanOnceItExpires() {
        assertFalse(account(AccountState.THROTTLED, 0, 200, NOW + 1).canTrack(NOW));
        assertTrue(account(AccountState.THROTTLED, 0, 200, NOW).canTrack(NOW));
        assertTrue(account(AccountState.THROTTLED, 0, 200, NOW - 1).canTrack(NOW));
    }

    @Test
    public void cannotTrackOnceTheLimitIsReached() {
        assertTrue(account(AccountState.OK, 199, 200, 0).canTrack(NOW));
        assertFalse(account(AccountState.OK, 200, 200, 0).canTrack(NOW));
        assertFalse(account(AccountState.OK, 250, 200, 0).canTrack(NOW));
    }

    @Test
    public void tracksWithoutBoundWhenTheLimitIsUnknown() {
        assertTrue(account(AccountState.OK, 5000, 0, 0).canTrack(NOW));
        assertTrue(account(AccountState.OK, 5000, -1, 0).canTrack(NOW));
    }

    @Test
    public void limitFallsBackToTheFreeTierWhenUnknown() {
        assertEquals(200, Account.FREE_TRACKING_LIMIT);
        assertEquals(200, account(AccountState.OK, 0, 0, 0).limitOrDefault());
        assertEquals(200, account(AccountState.OK, 0, -5, 0).limitOrDefault());
        assertEquals(1, account(AccountState.OK, 0, 1, 0).limitOrDefault());
        assertEquals(400, account(AccountState.OK, 0, 400, 0).limitOrDefault());
    }

}

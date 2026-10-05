/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class AccountsTest {

    private static final long NOW = 1_000_000_000_000L;

    private static JSONObject account(String id, String state, int tracked, int limit, long throttledUntil)
            throws JSONException {
        return new JSONObject()
                .put("id", id).put("token", id + "token").put("username", id)
                .put("state", state).put("tracked", tracked)
                .put("limit", limit).put("throttledUntil", throttledUntil);
    }

    private static Accounts accounts(String primaryId, String allocation, String owners, JSONObject... entries)
            throws JSONException {
        final JSONArray array = new JSONArray();
        for (JSONObject entry : entries) array.put(entry);
        return Accounts.parse(new JSONObject().put("version", 1).put("primaryId", primaryId)
                .put("allocation", allocation).put("accounts", array).toString(), owners);
    }

    private static Accounts accounts(String primaryId, String allocation, JSONObject... entries) throws JSONException {
        return accounts(primaryId, allocation, "{}", entries);
    }

    private static Accounts reload(Accounts accounts) throws JSONException {
        return Accounts.parse(accounts.toJson().toString(), accounts.owners().toString());
    }

    private static JSONObject overview(int count, int limit, String... asins) throws JSONException {
        final JSONArray trackings = new JSONArray();
        for (String asin : asins) trackings.put(new JSONObject().put("asin", asin));
        return new JSONObject().put("status", 200).put("trackingCount", count)
                .put("maxTrackingAllowed", limit).put("trackings", trackings);
    }

    @Test
    public void allocateFillsPrimaryThenStoreOrder() throws JSONException {
        final Accounts accounts = accounts("b", "auto",
                account("a", "ok", 10, 200, 0), account("b", "ok", 5, 200, 0));
        assertEquals("b", accounts.allocate("B01", NOW));
    }

    @Test
    public void allocateSkipsFullThrottledInvalid() throws JSONException {
        final Accounts accounts = accounts("a", "auto",
                account("a", "ok", 200, 200, 0),
                account("b", "throttled", 0, 200, NOW + 60000),
                account("c", "invalid", 0, 200, 0),
                account("d", "ok", 1, 200, 0));
        assertEquals("d", accounts.allocate("B01", NOW));
    }

    @Test
    public void allocateAcceptsExpiredThrottle() throws JSONException {
        assertEquals("a", accounts("a", "auto", account("a", "throttled", 0, 200, NOW - 1)).allocate("B01", NOW));
    }

    @Test
    public void allocateRoutesToExistingOwner() throws JSONException {
        final Accounts accounts = accounts("a", "auto", "{\"B01\":[\"b\"]}",
                account("a", "ok", 10, 200, 0), account("b", "ok", 10, 200, 0));
        assertEquals("b", accounts.allocate("B01", NOW));
    }

    @Test
    public void allocateHonoursOverrideThenFallsThrough() throws JSONException {
        assertEquals("b", accounts("a", "b", account("a", "ok", 0, 200, 0), account("b", "ok", 0, 200, 0))
                .allocate("B01", NOW));
        assertEquals("a", accounts("a", "b", account("a", "ok", 0, 200, 0), account("b", "ok", 200, 200, 0))
                .allocate("B01", NOW));
    }

    @Test
    public void allocateReturnsEmptyWhenAllFull() throws JSONException {
        assertEquals("", accounts("a", "auto", account("a", "ok", 200, 200, 0), account("b", "ok", 200, 200, 0))
                .allocate("B01", NOW));
    }

    @Test
    public void allocateWithoutPrimaryIdUsesStoreOrder() throws JSONException {
        assertEquals("b", accounts("", "auto", account("a", "ok", 200, 200, 0), account("b", "ok", 0, 200, 0))
                .allocate("B01", NOW));
    }

    @Test
    public void mergeConcatsDedupesAndSums() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 0, 200, 0), account("b", "ok", 0, 200, 0));
        final JSONObject responses = new JSONObject()
                .put("a", overview(2, 200, "X", "Y")
                        .put("products", new JSONArray().put(new JSONObject().put("asin", "X").put("domainId", 1))))
                .put("b", overview(1, 5000, "X")
                        .put("products", new JSONArray().put(new JSONObject().put("asin", "X").put("domainId", 1))));
        final JSONObject merged = accounts.mergeOverviews(responses, NOW);
        assertEquals(3, merged.getInt("trackingCount"));
        assertEquals(5200, merged.getInt("maxTrackingAllowed"));
        assertEquals(2, merged.getJSONArray("trackings").length());
        assertEquals(1, merged.getJSONArray("products").length());
        assertEquals("[\"a\",\"b\"]", accounts.owners().getJSONArray("X").toString());
        assertEquals("[\"a\"]", accounts.owners().getJSONArray("Y").toString());
    }

    @Test
    public void mergeKeepsStaleAccountOnFailedResponse() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 7, 200, 0), account("b", "ok", 3, 200, 0));
        accounts.mergeOverviews(new JSONObject().put("a", overview(8, 200, "X"))
                .put("b", new JSONObject().put("status", 505)), NOW);
        assertEquals(8, accounts.find("a").tracked);
        assertEquals(3, accounts.find("b").tracked);
    }

    @Test
    public void mergeKeepsOwnershipOfAccountsThatDidNotAnswer() throws JSONException {
        final Accounts accounts = accounts("a", "auto", "{\"X\":[\"a\",\"b\"],\"Z\":[\"b\"]}",
                account("a", "ok", 1, 200, 0), account("b", "ok", 2, 200, 0));
        accounts.mergeOverviews(new JSONObject().put("a", overview(1, 200, "X")), NOW);
        assertEquals("[\"a\",\"b\"]", accounts.owners().getJSONArray("X").toString());
        assertEquals("[\"b\"]", accounts.owners().getJSONArray("Z").toString());
        assertEquals("b", accounts.allocate("Z", NOW));
    }

    @Test
    public void mergeDropsOwnershipTheAnsweringAccountNoLongerReports() throws JSONException {
        final Accounts accounts = accounts("a", "auto", "{\"X\":[\"a\"]}", account("a", "ok", 1, 200, 0));
        accounts.mergeOverviews(new JSONObject().put("a", overview(0, 200)), NOW);
        assertFalse(accounts.owners().has("X"));
    }

    @Test
    public void mergeUsesFallbackLimitForDisplayWithoutPersistingIt() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 0, 0, 0), account("b", "ok", 0, 0, 0));
        final JSONObject merged = accounts.mergeOverviews(new JSONObject()
                .put("a", new JSONObject().put("status", 200))
                .put("b", overview(1, 200, "X")), NOW);
        assertEquals(1, merged.getInt("trackingCount"));
        assertEquals(Account.FREE_TRACKING_LIMIT + 200, merged.getInt("maxTrackingAllowed"));
        assertEquals(0, reload(accounts).find("a").limit);
        assertEquals(200, reload(accounts).find("b").limit);
    }

    @Test
    public void mergeDoesNotDuplicateOwnerForRepeatedTracking() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 0, 200, 0));
        accounts.mergeOverviews(new JSONObject().put("a", overview(2, 200, "X", "X")), NOW);
        assertEquals("[\"a\"]", accounts.owners().getJSONArray("X").toString());
    }

    @Test
    public void addResultIncrementsOnlyForNewAsin() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 10, 200, 0));
        accounts.applyAddResult("B01", "a", true, "");
        assertEquals(11, accounts.find("a").tracked);
        final Accounts reloaded = reload(accounts);
        reloaded.applyAddResult("B01", "a", true, "");
        assertEquals(11, reloaded.find("a").tracked);
    }

    @Test
    public void addCountsEachOwnerOnce() throws JSONException {
        final Accounts accounts = accounts("a", "auto", "{\"X\":[\"a\"]}",
                account("a", "ok", 1, 200, 0), account("b", "ok", 0, 200, 0));
        accounts.applyAddResult("X", "b", true, "");
        assertEquals(1, accounts.find("b").tracked);
        assertEquals("[\"a\",\"b\"]", accounts.owners().getJSONArray("X").toString());
    }

    @Test
    public void addResultMarksFullOnMaxReached() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 198, 200, 0));
        accounts.applyAddResult("B01", "a", false, "maxTrackingReached");
        assertEquals(200, accounts.find("a").tracked);
    }

    @Test
    public void rejectionWithUnknownLimitAllowsFallback() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 0, 0, 0), account("b", "ok", 0, 200, 0));
        accounts.applyAddResult("X", "a", false, "maxTrackingReached");
        assertEquals("b", accounts.allocate("X", NOW));
    }

    @Test
    public void rejectTransitionsState() throws JSONException {
        final Accounts invalid = accounts("a", "auto", account("a", "ok", 10, 200, 0));
        invalid.applyReject("a", 401, NOW);
        assertEquals(AccountState.INVALID, invalid.find("a").state);

        final Accounts throttled = accounts("a", "auto", account("a", "ok", 10, 200, 0));
        throttled.applyReject("a", 429, NOW);
        assertEquals("throttled", reload(throttled).toJson().getJSONArray("accounts").getJSONObject(0)
                .getString("state"));
        assertEquals(NOW + Accounts.THROTTLE_MS, throttled.find("a").throttledUntil);
    }

    @Test
    public void deletionFreesCapacityOnlyOnce() throws JSONException {
        final Accounts accounts = accounts("a", "auto", "{\"X\":[\"a\"]}", account("a", "ok", 200, 200, 0));
        accounts.applyDelete("X", "a");
        accounts.applyDelete("X", "a");
        assertEquals(199, accounts.find("a").tracked);
        assertEquals("a", accounts.allocate("Y", NOW));
        assertFalse(accounts.owners().has("X"));
    }

    @Test
    public void deleteRemovesOneOwnerThenAsinAndIgnoresUnknownOwners() throws JSONException {
        final Accounts accounts = accounts("a", "auto", "{\"X\":[\"a\",\"b\"]}",
                account("a", "ok", 1, 200, 0), account("b", "ok", 1, 200, 0));
        accounts.applyDelete("X", "c");
        accounts.applyDelete("Y", "a");
        assertEquals("[\"a\",\"b\"]", accounts.owners().getJSONArray("X").toString());
        accounts.applyDelete("X", "a");
        assertEquals("[\"b\"]", accounts.owners().getJSONArray("X").toString());
        accounts.applyDelete("X", "b");
        assertFalse(accounts.owners().has("X"));
    }

    @Test
    public void upsertUpdatesExistingUsernameAndPreservesIdentityAndMetadata() throws JSONException {
        final Accounts accounts = Accounts.parse(accounts("b", "b",
                account("a", "invalid", 5, 200, NOW + 1000).put("addedAt", NOW - 1000),
                account("b", "ok", 0, 200, 0)).toJson().put("futureField", "preserved").toString(), "{}");
        accounts.upsertByUsername("replacement", "a", "a@example.test", NOW);
        final JSONObject document = reload(accounts).toJson();
        assertEquals(2, document.getJSONArray("accounts").length());
        assertEquals("a", document.getString("primaryId"));
        assertEquals("b", document.getString("allocation"));
        assertEquals(1, document.getInt("version"));
        assertEquals("preserved", document.getString("futureField"));
        final JSONObject account = document.getJSONArray("accounts").getJSONObject(0);
        assertEquals("a", account.getString("id"));
        assertEquals("replacement", account.getString("token"));
        assertEquals("a@example.test", account.getString("email"));
        assertEquals("ok", account.getString("state"));
        assertEquals(0, account.getLong("throttledUntil"));
        assertEquals(NOW - 1000, account.getLong("addedAt"));
        assertEquals(5, account.getInt("tracked"));
    }

    @Test
    public void upsertAddsNewAccountWithFreshIdAndMakesItPrimary() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 0, 200, 0));
        accounts.upsertByUsername("token", "new", "", NOW);
        assertEquals(2, accounts.all().size());
        final Account added = accounts.all().get(1);
        assertNotEquals("a", added.id);
        assertFalse(added.id.isEmpty());
        assertEquals(added.id, accounts.primaryId());
        assertEquals(NOW, added.addedAt);
    }

    @Test
    public void upsertWithDuplicateUsernamesUpdatesTheFirstOnly() throws JSONException {
        final Accounts accounts = accounts("b", "auto",
                account("a", "ok", 0, 200, 0).put("username", "same"),
                account("b", "ok", 0, 200, 0).put("username", "same"));
        accounts.upsertByUsername("replacement", "same", "", NOW);
        assertEquals("replacement", accounts.find("a").token);
        assertEquals("btoken", accounts.find("b").token);
        assertEquals("a", accounts.primaryId());
    }

    @Test
    public void updateTokenDoesNotInsertUnknownAccountAndClearsThrottle() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "throttled", 5, 200, NOW + 1000));
        accounts.updateToken("b", "replacement");
        assertEquals(1, accounts.all().size());
        assertEquals("atoken", accounts.find("a").token);
        accounts.updateToken("a", "replacement");
        assertEquals("replacement", accounts.find("a").token);
        assertEquals(AccountState.OK, accounts.find("a").state);
        assertEquals(0, accounts.find("a").throttledUntil);
    }

    @Test
    public void removePrimaryPrefersHealthyAccountAndEventuallyClearsPrimaryId() throws JSONException {
        final Accounts accounts = accounts("a", "auto",
                account("a", "ok", 0, 200, 0), account("b", "invalid", 0, 200, 0), account("c", "ok", 0, 200, 0));
        accounts.remove("a");
        assertEquals("c", accounts.primaryId());
        accounts.remove("c");
        assertEquals("b", accounts.primaryId());
        accounts.remove("b");
        assertEquals("", accounts.primaryId());
        assertEquals(0, reload(accounts).toJson().getJSONArray("accounts").length());
    }

    @Test
    public void removeDropsEveryOwnershipOfTheAccountAndKeepsOtherOwnersInOrder() throws JSONException {
        final Accounts accounts = accounts("b", "auto", "{\"X\":[\"a\",\"b\",\"a\",\"c\"],\"Y\":[\"a\"],\"Z\":[\"b\"]}",
                account("a", "ok", 0, 200, 0), account("b", "ok", 0, 200, 0));
        accounts.remove("a");
        assertEquals("[\"b\",\"c\"]", accounts.owners().getJSONArray("X").toString());
        assertFalse(accounts.owners().has("Y"));
        assertEquals("[\"b\"]", accounts.owners().getJSONArray("Z").toString());
    }

    @Test
    public void setPrimaryIgnoresUnknownId() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "ok", 0, 200, 0));
        accounts.setPrimary("missing");
        assertEquals("a", accounts.primaryId());
    }

    @Test
    public void allocationRequiresAnExistingAccountOrAuto() throws JSONException {
        final Accounts accounts = accounts("a", "a", account("a", "ok", 0, 200, 0));
        accounts.setAllocation("missing");
        assertEquals("a", accounts.allocationId());
        accounts.remove("a");
        assertEquals("auto", accounts.allocationId());
        accounts.setAllocation("auto");
        assertEquals("auto", accounts.allocationId());
    }

    @Test
    public void summaryReflectsCountAndAccountsWithFallbackLimit() throws JSONException {
        assertEquals("", accounts("", "auto").summary());
        assertEquals("198 / 200 tracked", accounts("a", "auto", account("a", "ok", 198, 200, 0)).summary());
        final Accounts two = accounts("a", "auto", account("a", "ok", 198, 5000, 0), account("b", "ok", 5, 0, 0));
        assertEquals("2 accounts, 203 / 5200 tracked", two.summary());
        assertEquals(203, two.trackedTotal());
        assertEquals(5000 + Account.FREE_TRACKING_LIMIT, two.limitTotal());
    }

    @Test
    public void emptyDocumentLoadsAndSerializesDefaults() throws JSONException {
        final JSONObject document = Accounts.parse("{}", "{}").toJson();
        assertEquals("", document.getString("primaryId"));
        assertEquals("auto", document.getString("allocation"));
        assertEquals(0, document.getJSONArray("accounts").length());
    }

    @Test
    public void unknownFieldsAndNewerVersionSurviveARoundTrip() throws JSONException {
        final String stored = new JSONObject().put("version", 2).put("tagProducts", false).put("primaryId", "a")
                .put("accounts", new JSONArray().put(account("a", "ok", 0, 200, 0).put("accountType", 2)
                        .put("futureAccountField", "kept")))
                .toString();
        final JSONObject document = Accounts.parse(stored, "{}").toJson();
        assertEquals(2, document.getInt("version"));
        assertFalse(document.getBoolean("tagProducts"));
        final JSONObject account = document.getJSONArray("accounts").getJSONObject(0);
        assertEquals("kept", account.getString("futureAccountField"));
        assertEquals(2, account.getInt("accountType"));
    }

    @Test
    public void accountDefaultsPreserveDocumentShape() throws JSONException {
        final JSONObject restored = Account.fromJson(new JSONObject().put("id", "a")).toJson();
        assertEquals(10, restored.length());
        assertEquals("", restored.getString("token"));
        assertEquals("ok", restored.getString("state"));
        for (String field : new String[]{"throttledUntil", "tracked", "limit", "refreshedAt", "addedAt"}) {
            assertEquals(0, restored.getLong(field));
        }
    }

    @Test
    public void unknownStateLoadsAsInvalidAndIsNeverAllocated() throws JSONException {
        final Accounts accounts = accounts("a", "auto", account("a", "suspended", 0, 200, 0));
        assertEquals(AccountState.INVALID, accounts.find("a").state);
        assertEquals("", accounts.allocate("X", NOW));
    }

    @Test
    public void canTrackDoesNotMutateExpiredThrottle() throws JSONException {
        final Account account = Account.fromJson(account("a", "throttled", 1, 200, NOW));
        assertFalse(account.canTrack(NOW - 1));
        assertTrue(account.canTrack(NOW));
        assertEquals(AccountState.THROTTLED, account.state);
        assertEquals(NOW, account.throttledUntil);
    }

    @Test(expected = JSONException.class)
    public void malformedDocumentFailsLoudly() throws JSONException {
        Accounts.parse("AAAAbase64ciphertext==", "{}");
    }

    @Test(expected = JSONException.class)
    public void accountWithoutIdFailsLoudly() throws JSONException {
        Accounts.parse("{\"accounts\":[{\"username\":\"a\"}]}", "{}");
    }
}

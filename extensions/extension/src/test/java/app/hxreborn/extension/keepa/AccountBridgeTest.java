/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public final class AccountBridgeTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private AccountStore store;

    @Before
    public void seedTwoAccounts() throws Exception {
        store = new AccountStore(new File(folder.getRoot(), AccountStore.FILE_NAME));
        call("onServerData", new JSONObject().put("token", token('a')).put("username", "alice").put("email", "a@x"));
        call("onServerData", new JSONObject().put("token", token('b')).put("username", "bob").put("email", "b@x"));
    }

    private static String token(char fill) {
        return new String(new char[64]).replace('\0', fill);
    }

    private Object call(String method, JSONObject arguments) throws JSONException {
        return AccountBridge.dispatch(null, store, method, arguments == null ? "" : arguments.toString());
    }

    private JSONObject snapshot() throws JSONException {
        return (JSONObject) call("load", null);
    }

    private String idOf(String username) throws JSONException {
        final JSONArray accounts = snapshot().getJSONObject("accounts").getJSONArray("accounts");
        for (int i = 0; i < accounts.length(); i++) {
            if (accounts.getJSONObject(i).getString("username").equals(username)) return accounts.getJSONObject(i).getString("id");
        }
        throw new AssertionError("no account " + username);
    }

    @Test
    public void snapshotCarriesEveryFieldTheRuntimeReads() throws JSONException {
        final JSONObject snapshot = snapshot();
        for (String key : new String[]{"accounts", "owners", "pending", "summary", "limitTotal"}) {
            assertTrue(key, snapshot.has(key));
        }
        assertEquals(JSONObject.NULL, snapshot.get("pending"));
        assertEquals("2 accounts, 0 / 400 tracked", snapshot.getString("summary"));
        assertEquals(400, snapshot.getInt("limitTotal"));
        assertEquals(idOf("bob"), snapshot.getJSONObject("accounts").getString("primaryId"));
    }

    @Test
    public void pendingRoundTripsAsAnObject() throws JSONException {
        call("setPending", new JSONObject().put("op", "switch").put("id", idOf("alice")));
        assertEquals("switch", snapshot().getJSONObject("pending").getString("op"));
        call("clearPending", null);
        assertEquals(JSONObject.NULL, snapshot().get("pending"));
    }

    @Test
    public void mergeCommitsOnlyWhenAsked() throws JSONException {
        final JSONObject responses = new JSONObject().put(idOf("alice"), new JSONObject().put("status", 200)
                .put("trackingCount", 3).put("maxTrackingAllowed", 200)
                .put("trackings", new JSONArray().put(new JSONObject().put("asin", "X"))));
        final JSONObject uncommitted = (JSONObject) call("mergeOverviews",
                new JSONObject().put("responses", responses).put("commit", false));
        assertEquals(3, uncommitted.getInt("trackingCount"));
        assertFalse(snapshot().getJSONObject("owners").has("X"));

        call("mergeOverviews", new JSONObject().put("responses", responses).put("commit", true));
        assertEquals(idOf("alice"), snapshot().getJSONObject("owners").getJSONArray("X").getString(0));
        assertEquals("2 accounts, 3 / 400 tracked", snapshot().getString("summary"));
    }

    @Test
    public void addDeleteAndAllocateUseTheRuntimeArgumentShapes() throws JSONException {
        assertEquals(idOf("bob"), call("allocate", new JSONObject().put("asin", "Y")));
        call("applyAddResult", new JSONObject().put("asin", "Y").put("accountId", idOf("bob")).put("ok", true));
        assertEquals(idOf("bob"), snapshot().getJSONObject("owners").getJSONArray("Y").getString(0));
        call("applyDelete", new JSONObject().put("accountId", idOf("bob")).put("asins", new JSONArray().put("Y")));
        assertFalse(snapshot().getJSONObject("owners").has("Y"));
    }

    @Test
    public void rejectInvalidateAndRemoveChangeAccountState() throws JSONException {
        call("applyReject", new JSONObject().put("accountId", idOf("alice")).put("status", 401));
        assertEquals("invalid", accountNamed("alice").getString("state"));
        call("upsertToken", new JSONObject().put("id", idOf("alice")).put("token", token('c')));
        assertEquals("ok", accountNamed("alice").getString("state"));
        call("invalidate", new JSONObject().put("id", idOf("bob")));
        assertEquals("invalid", accountNamed("bob").getString("state"));
        call("setPrimary", new JSONObject().put("id", idOf("alice")));
        call("remove", new JSONObject().put("id", idOf("bob")));
        assertEquals(1, snapshot().getJSONObject("accounts").getJSONArray("accounts").length());
        assertEquals("0 / 200 tracked", snapshot().getString("summary"));
    }

    @Test
    public void unknownMethodIsRejected() {
        try {
            call("nope", null);
            throw new AssertionError("expected a failure");
        } catch (JSONException | IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("nope"));
        }
    }

    private JSONObject accountNamed(String username) throws JSONException {
        final JSONArray accounts = snapshot().getJSONObject("accounts").getJSONArray("accounts");
        for (int i = 0; i < accounts.length(); i++) {
            if (accounts.getJSONObject(i).getString("username").equals(username)) return accounts.getJSONObject(i);
        }
        throw new AssertionError("no account " + username);
    }
}

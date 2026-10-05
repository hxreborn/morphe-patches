/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import org.json.JSONException;
import org.json.JSONObject;

final class Account {

    static final int FREE_TRACKING_LIMIT = 200;

    final String id;
    String token = "";
    String username = "";
    String email = "";
    AccountState state = AccountState.OK;
    long throttledUntil;
    int tracked;
    int limit;
    long refreshedAt;
    long addedAt;
    private final JSONObject json;

    Account(String id) {
        this(id, new JSONObject());
    }

    private Account(String id, JSONObject json) {
        this.id = id;
        this.json = json;
    }

    static Account fromJson(JSONObject json) throws JSONException {
        final Account account = new Account(json.getString("id"), json);
        account.token = json.optString("token", "");
        account.username = json.optString("username", "");
        account.email = json.optString("email", "");
        account.state = AccountState.fromWireValue(json.optString("state", AccountState.OK.wireValue));
        account.throttledUntil = json.optLong("throttledUntil", 0);
        account.tracked = json.optInt("tracked", 0);
        account.limit = json.optInt("limit", 0);
        account.refreshedAt = json.optLong("refreshedAt", 0);
        account.addedAt = json.optLong("addedAt", 0);
        return account;
    }

    JSONObject toJson() throws JSONException {
        return json.put("id", id)
                .put("token", token)
                .put("username", username)
                .put("email", email)
                .put("state", state.wireValue)
                .put("throttledUntil", throttledUntil)
                .put("tracked", tracked)
                .put("limit", limit)
                .put("refreshedAt", refreshedAt)
                .put("addedAt", addedAt);
    }

    void markHealthy() {
        state = AccountState.OK;
        throttledUntil = 0;
    }

    boolean isThrottled(long now) {
        return state == AccountState.THROTTLED && now < throttledUntil;
    }

    boolean canTrack(long now) {
        return state != AccountState.INVALID && !isThrottled(now) && (limit <= 0 || tracked < limit);
    }

    int limitOrDefault() {
        return limit > 0 ? limit : FREE_TRACKING_LIMIT;
    }
}

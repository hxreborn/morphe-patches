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
        return this.json.put("id", this.id)
            .put("token", this.token)
            .put("username", this.username)
            .put("email", this.email)
            .put("state", this.state.wireValue)
            .put("throttledUntil", this.throttledUntil)
            .put("tracked", this.tracked)
            .put("limit", this.limit)
            .put("refreshedAt", this.refreshedAt)
            .put("addedAt", this.addedAt);
    }

    void markHealthy() {
        this.state = AccountState.OK;
        this.throttledUntil = 0;
    }

    boolean isThrottled(long now) {
        return this.state == AccountState.THROTTLED && now < this.throttledUntil;
    }

    boolean canTrack(long now) {
        return this.state != AccountState.INVALID && !isThrottled(now)
                && (this.limit <= 0 || this.tracked < this.limit);
    }

    int limitOrDefault() {
        return (this.limit > 0) ? this.limit : FREE_TRACKING_LIMIT;
    }

}

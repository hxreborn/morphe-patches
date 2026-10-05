/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class Accounts {

    private static final long MINUTE_MS = 60_000L;
    static final long THROTTLE_MS = 15 * MINUTE_MS;
    static final String AUTOMATIC = "auto";
    static final String TAG_BELOW = "below";
    static final String TAG_FLAG = "flag";
    static final String TAG_OFF = "off";

    private final JSONObject document;
    private JSONObject owners;
    private final List<Account> accounts = new ArrayList<>();

    private Accounts(JSONObject document, JSONObject owners) throws JSONException {
        this.document = document;
        this.owners = owners;
        final JSONArray array = document.has("accounts") ? document.getJSONArray("accounts") : new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            accounts.add(Account.fromJson(array.getJSONObject(i)));
        }
    }

    static Accounts parse(String documentJson, String ownersJson) throws JSONException {
        return new Accounts(new JSONObject(documentJson), new JSONObject(ownersJson));
    }

    JSONObject toJson() throws JSONException {
        final JSONArray array = new JSONArray();
        for (Account account : accounts) array.put(account.toJson());
        return document.put("primaryId", primaryId()).put("allocation", allocationId()).put("accounts", array);
    }

    JSONObject owners() {
        return owners;
    }

    List<Account> all() {
        return accounts;
    }

    String primaryId() {
        return document.optString("primaryId", "");
    }

    String allocationId() {
        return document.optString("allocation", AUTOMATIC);
    }

    String tagPlacement() {
        return document.optString("tagPlacement", TAG_BELOW);
    }

    void setTagPlacement(String placement) throws JSONException {
        if (TAG_BELOW.equals(placement) || TAG_FLAG.equals(placement) || TAG_OFF.equals(placement)) {
            document.put("tagPlacement", placement);
        }
    }

    void invalidate(String id) {
        final Account account = find(id);
        if (account != null) account.state = AccountState.INVALID;
    }

    Account find(String id) {
        for (Account account : accounts) if (account.id.equals(id)) return account;
        return null;
    }

    int trackedTotal() {
        int total = 0;
        for (Account account : accounts) total += account.tracked;
        return total;
    }

    int limitTotal() {
        int total = 0;
        for (Account account : accounts) total += account.limitOrDefault();
        return total;
    }

    String summary() {
        if (accounts.isEmpty()) return "";
        final String counts = trackedTotal() + " / " + limitTotal() + " tracked";
        return accounts.size() == 1 ? counts : accounts.size() + " accounts, " + counts;
    }

    String allocate(String asin, long now) throws JSONException {
        final JSONArray existing = owners.optJSONArray(asin);
        if (existing != null && existing.length() > 0) return existing.getString(0);
        final Account selected = find(allocationId());
        if (selected != null && selected.canTrack(now)) return selected.id;
        final Account primary = find(primaryId());
        if (primary != null && primary.canTrack(now)) return primary.id;
        for (Account account : accounts) if (account.canTrack(now)) return account.id;
        return "";
    }

    JSONObject mergeOverviews(JSONObject responses, long now) throws JSONException {
        final Map<String, JSONObject> trackingsByAsin = new LinkedHashMap<>();
        final Map<String, JSONObject> productsByKey = new LinkedHashMap<>();
        final JSONObject previousOwners = owners;
        owners = new JSONObject();
        int totalCount = 0;
        int totalLimit = 0;

        for (Account account : accounts) {
            final JSONObject response = responses.optJSONObject(account.id);
            if (response == null || response.optInt("status", 0) != 200 || response.has("error")) {
                keepOwnership(previousOwners, account.id);
                continue;
            }
            final int reportedLimit = response.optInt("maxTrackingAllowed", 0);
            if (reportedLimit > 0) account.limit = reportedLimit;
            account.tracked = response.optInt("trackingCount", 0);
            account.refreshedAt = now;
            if (account.state == AccountState.THROTTLED && !account.isThrottled(now)) account.markHealthy();
            totalCount += account.tracked;
            totalLimit += account.limitOrDefault();

            final JSONArray trackings = response.optJSONArray("trackings");
            for (int i = 0; trackings != null && i < trackings.length(); i++) {
                final JSONObject tracking = trackings.getJSONObject(i);
                final String asin = tracking.optString("asin", "");
                if (asin.isEmpty()) continue;
                if (!trackingsByAsin.containsKey(asin)) trackingsByAsin.put(asin, tracking);
                addOwner(asin, account.id);
            }
            addProducts(productsByKey, response.optJSONArray("products"));
            addProducts(productsByKey, response.optJSONArray("noHistoryProducts"));
        }

        return new JSONObject()
                .put("status", 200)
                .put("trackingCount", totalCount)
                .put("maxTrackingAllowed", totalLimit)
                .put("trackings", new JSONArray(trackingsByAsin.values()))
                .put("products", new JSONArray(productsByKey.values()));
    }

    void applyAddResult(String asin, String accountId, boolean succeeded, String errorType) throws JSONException {
        final Account account = find(accountId);
        if (account == null) return;
        if (succeeded) {
            if (addOwner(asin, accountId)) account.tracked += 1;
            account.markHealthy();
        } else if ("maxTrackingReached".equals(errorType)) {
            if (account.limit <= 0) account.limit = Math.max(account.tracked, 1);
            account.tracked = Math.max(account.tracked, account.limit);
        }
    }

    void applyReject(String accountId, int status, long now) {
        final Account account = find(accountId);
        if (account == null) return;
        if (status == 401) {
            account.state = AccountState.INVALID;
        } else if (status == 429) {
            account.state = AccountState.THROTTLED;
            account.throttledUntil = now + THROTTLE_MS;
        }
    }

    void applyDelete(String asin, String accountId) throws JSONException {
        final Account account = find(accountId);
        if (removeOwner(asin, accountId) && account != null) {
            account.tracked = Math.max(0, account.tracked - 1);
        }
    }

    void upsertByUsername(String token, String username, String email, long now) throws JSONException {
        Account account = null;
        for (Account candidate : accounts) {
            if (candidate.username.equals(username)) {
                account = candidate;
                break;
            }
        }
        if (account == null) {
            account = new Account(UUID.randomUUID().toString());
            account.username = username;
            account.addedAt = now;
            accounts.add(account);
        }
        account.token = token;
        account.email = email;
        account.markHealthy();
        document.put("primaryId", account.id);
    }

    void updateToken(String id, String token) {
        final Account account = find(id);
        if (account == null) return;
        account.token = token;
        account.markHealthy();
    }

    void setPrimary(String id) throws JSONException {
        if (find(id) != null) document.put("primaryId", id);
    }

    void setAllocation(String allocation) throws JSONException {
        if (AUTOMATIC.equals(allocation) || find(allocation) != null) document.put("allocation", allocation);
    }

    void remove(String id) throws JSONException {
        final Account removed = find(id);
        if (removed == null) return;
        accounts.remove(removed);
        final List<String> asins = new ArrayList<>();
        final Iterator<String> keys = owners.keys();
        while (keys.hasNext()) asins.add(keys.next());
        for (String asin : asins) removeOwner(asin, id);
        if (id.equals(primaryId())) document.put("primaryId", nextPrimaryId());
        if (id.equals(allocationId())) document.put("allocation", AUTOMATIC);
    }

    private String nextPrimaryId() {
        for (Account account : accounts) if (account.state == AccountState.OK) return account.id;
        return accounts.isEmpty() ? "" : accounts.get(0).id;
    }

    private void keepOwnership(JSONObject previousOwners, String accountId) throws JSONException {
        final Iterator<String> asins = previousOwners.keys();
        while (asins.hasNext()) {
            final String asin = asins.next();
            if (containsId(previousOwners.getJSONArray(asin), accountId)) addOwner(asin, accountId);
        }
    }

    private boolean addOwner(String asin, String accountId) throws JSONException {
        JSONArray ids = owners.optJSONArray(asin);
        if (ids == null) {
            ids = new JSONArray();
            owners.put(asin, ids);
        }
        if (containsId(ids, accountId)) return false;
        ids.put(accountId);
        return true;
    }

    private boolean removeOwner(String asin, String accountId) throws JSONException {
        final JSONArray ids = owners.optJSONArray(asin);
        if (ids == null || !containsId(ids, accountId)) return false;
        final JSONArray remaining = new JSONArray();
        for (int i = 0; i < ids.length(); i++) {
            if (!ids.getString(i).equals(accountId)) remaining.put(ids.getString(i));
        }
        if (remaining.length() == 0) owners.remove(asin);
        else owners.put(asin, remaining);
        return true;
    }

    private static boolean containsId(JSONArray ids, String id) throws JSONException {
        for (int i = 0; i < ids.length(); i++) if (ids.getString(i).equals(id)) return true;
        return false;
    }

    private static void addProducts(Map<String, JSONObject> productsByKey, JSONArray products) throws JSONException {
        for (int i = 0; products != null && i < products.length(); i++) {
            final JSONObject product = products.getJSONObject(i);
            final String key = product.optString("asin", "") + ":" + product.optInt("domainId", 0);
            if (!productsByKey.containsKey(key)) productsByKey.put(key, product);
        }
    }
}

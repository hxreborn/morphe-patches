/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class RevenueCatUnlock {

    private static final String PURCHASE_DATE = "2020-01-01T00:00:00Z";

    private static final String EXPIRES_DATE = "2099-01-01T00:00:00Z";

    private RevenueCatUnlock() {

    }

    public static void grantEntitlement(JSONObject body, String entitlement, String product) {
        if (body == null) {
            return;
        }

        try {
            JSONObject subscriber = body.optJSONObject("subscriber");
            if (subscriber == null) {
                return;
            }

            JSONObject subscriptions = getOrCreateObject(subscriber, "subscriptions");
            if (!subscriptions.has(product)) {
                subscriptions.put(product, buildSubscription());
            }

            JSONObject entitlements = getOrCreateObject(subscriber, "entitlements");
            entitlements.put(entitlement, buildEntitlement(product));

            renew(subscriptions);
            renew(entitlements);
        } catch (Exception ignored) {
        }
    }

    public static void grantLifetimeEntitlement(JSONObject body, String entitlement, String product) {
        if (body == null) {
            return;
        }

        try {
            JSONObject subscriber = body.optJSONObject("subscriber");
            if (subscriber == null) {
                return;
            }

            JSONObject nonSubscriptions = getOrCreateObject(subscriber, "non_subscriptions");
            if (!nonSubscriptions.has(product)) {
                nonSubscriptions.put(product, new JSONArray().put(buildNonSubscriptionTransaction()));
            }

            getOrCreateObject(subscriber, "entitlements").put(entitlement, buildLifetimeEntitlement(product));
        } catch (Exception ignored) {
        }
    }

    private static JSONObject getOrCreateObject(JSONObject parent, String name) throws JSONException {
        JSONObject child = parent.optJSONObject(name);
        if (child == null) {
            child = new JSONObject();
            parent.put(name, child);
        }
        return child;
    }

    private static void renew(JSONObject entriesById) throws JSONException {
        for (Iterator<String> keys = entriesById.keys(); keys.hasNext();) {
            JSONObject entry = entriesById.optJSONObject(keys.next());
            if (entry == null) {
                continue;
            }
            entry.put("expires_date", EXPIRES_DATE);
            if (entry.has("grace_period_expires_date")) {
                entry.put("grace_period_expires_date", EXPIRES_DATE);
            }
            entry.remove("billing_issues_detected_at");
            entry.remove("unsubscribe_detected_at");
        }
    }

    private static JSONObject buildSubscription() throws JSONException {
        JSONObject subscription = new JSONObject();
        subscription.put("purchase_date", PURCHASE_DATE);
        subscription.put("original_purchase_date", PURCHASE_DATE);
        subscription.put("expires_date", EXPIRES_DATE);
        subscription.put("store", "play_store");
        subscription.put("is_sandbox", false);
        subscription.put("period_type", "normal");
        subscription.put("ownership_type", "PURCHASED");
        return subscription;
    }

    private static JSONObject buildEntitlement(String product) throws JSONException {
        JSONObject entitlement = new JSONObject();
        entitlement.put("product_identifier", product);
        entitlement.put("purchase_date", PURCHASE_DATE);
        entitlement.put("expires_date", EXPIRES_DATE);
        return entitlement;
    }

    private static JSONObject buildNonSubscriptionTransaction() throws JSONException {
        JSONObject transaction = new JSONObject();
        transaction.put("id", "hx_lifetime");
        transaction.put("purchase_date", PURCHASE_DATE);
        transaction.put("original_purchase_date", PURCHASE_DATE);
        transaction.put("store", "play_store");
        transaction.put("is_sandbox", false);
        return transaction;
    }

    private static JSONObject buildLifetimeEntitlement(String product) throws JSONException {
        JSONObject entitlement = new JSONObject();
        entitlement.put("product_identifier", product);
        entitlement.put("purchase_date", PURCHASE_DATE);
        entitlement.put("expires_date", JSONObject.NULL);
        return entitlement;
    }

}

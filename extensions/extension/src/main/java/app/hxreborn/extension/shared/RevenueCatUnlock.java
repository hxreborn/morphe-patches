/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class RevenueCatUnlock {
    private static final String PURCHASE_DATE = "2020-01-01T00:00:00Z";
    private static final String EXPIRES_DATE = "2099-01-01T00:00:00Z";

    private RevenueCatUnlock() {}

    public static void grantEntitlement(JSONObject body, String entitlement, String product) {
        if (body == null) {
            return;
        }

        try {
            JSONObject subscriber = body.optJSONObject("subscriber");
            if (subscriber == null) {
                return;
            }

            JSONObject subscriptions = subscriber.optJSONObject("subscriptions");
            if (subscriptions == null) {
                subscriptions = new JSONObject();
                subscriber.put("subscriptions", subscriptions);
            }
            if (!subscriptions.has(product)) {
                subscriptions.put(product, buildSubscription());
            }

            JSONObject entitlements = subscriber.optJSONObject("entitlements");
            if (entitlements == null) {
                entitlements = new JSONObject();
                subscriber.put("entitlements", entitlements);
            }
            entitlements.put(entitlement, buildEntitlement(product));
        } catch (Exception ignored) {
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
}

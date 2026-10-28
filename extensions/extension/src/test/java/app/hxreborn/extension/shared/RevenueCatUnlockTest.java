/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class RevenueCatUnlockTest {

    private static final String ENTITLEMENT = "pro";

    private static final String PRODUCT = "pro_yearly";

    private static final String PURCHASE_DATE = "2020-01-01T00:00:00Z";

    private static final String EXPIRES_DATE = "2099-01-01T00:00:00Z";

    private static JSONObject body(JSONObject subscriber) throws JSONException {
        return new JSONObject().put("request_date", "2026-01-01T00:00:00Z").put("subscriber", subscriber);
    }

    private static JSONObject grant(JSONObject body) {
        RevenueCatUnlock.grantEntitlement(body, ENTITLEMENT, PRODUCT);
        return body;
    }

    private static JSONObject subscriber(JSONObject body) throws JSONException {
        return body.getJSONObject("subscriber");
    }

    @Test
    public void ignoresNullBody() {
        RevenueCatUnlock.grantEntitlement(null, ENTITLEMENT, PRODUCT);
    }

    @Test
    public void leavesBodyWithoutSubscriberUntouched() throws JSONException {
        // given
        final JSONObject body = new JSONObject().put("other", 1);

        // when
        final JSONObject granted = grant(body);

        // then
        assertEquals("{\"other\":1}", granted.toString());
    }

    @Test
    public void leavesBodyWhoseSubscriberIsNotAnObjectUntouched() throws JSONException {
        // given
        final JSONObject body = new JSONObject().put("subscriber", "none");

        // when
        final JSONObject granted = grant(body);

        // then
        assertEquals("none", granted.getString("subscriber"));
    }

    @Test
    public void createsBothSectionsForAnEmptySubscriber() throws JSONException {
        // when
        final JSONObject subscriber = subscriber(grant(body(new JSONObject())));

        // then
        final JSONObject entitlement = subscriber.getJSONObject("entitlements").getJSONObject(ENTITLEMENT);
        assertEquals(PRODUCT, entitlement.getString("product_identifier"));
        assertEquals(PURCHASE_DATE, entitlement.getString("purchase_date"));
        assertEquals(EXPIRES_DATE, entitlement.getString("expires_date"));

        final JSONObject subscription = subscriber.getJSONObject("subscriptions").getJSONObject(PRODUCT);
        assertEquals(PURCHASE_DATE, subscription.getString("purchase_date"));
        assertEquals(PURCHASE_DATE, subscription.getString("original_purchase_date"));
        assertEquals(EXPIRES_DATE, subscription.getString("expires_date"));
        assertEquals("play_store", subscription.getString("store"));
        assertFalse(subscription.getBoolean("is_sandbox"));
        assertEquals("normal", subscription.getString("period_type"));
        assertEquals("PURCHASED", subscription.getString("ownership_type"));
    }

    @Test
    public void extendsAnExistingSubscriptionInPlace() throws JSONException {
        // given
        final JSONObject existing = new JSONObject().put("store", "app_store")
            .put("purchase_date", "2025-05-05T00:00:00Z")
            .put("expires_date", "2025-06-05T00:00:00Z")
            .put("grace_period_expires_date", "2025-06-10T00:00:00Z")
            .put("billing_issues_detected_at", "2025-06-06T00:00:00Z")
            .put("unsubscribe_detected_at", "2025-06-07T00:00:00Z");
        final JSONObject subscriber = new JSONObject().put("subscriptions", new JSONObject().put(PRODUCT, existing));

        // when
        final JSONObject subscription = subscriber(grant(body(subscriber))).getJSONObject("subscriptions")
            .getJSONObject(PRODUCT);

        // then
        assertEquals("app_store", subscription.getString("store"));
        assertEquals("2025-05-05T00:00:00Z", subscription.getString("purchase_date"));
        assertEquals(EXPIRES_DATE, subscription.getString("expires_date"));
        assertEquals(EXPIRES_DATE, subscription.getString("grace_period_expires_date"));
        assertFalse(subscription.has("billing_issues_detected_at"));
        assertFalse(subscription.has("unsubscribe_detected_at"));
    }

    @Test
    public void addsNoGracePeriodWhereThereWasNone() throws JSONException {
        // given
        final JSONObject subscriber = new JSONObject().put("subscriptions",
                new JSONObject().put(PRODUCT, new JSONObject().put("expires_date", "2025-06-05T00:00:00Z")));

        // when
        final JSONObject subscription = subscriber(grant(body(subscriber))).getJSONObject("subscriptions")
            .getJSONObject(PRODUCT);

        // then
        assertFalse(subscription.has("grace_period_expires_date"));
    }

    @Test
    public void extendsUnrelatedEntriesInBothSections() throws JSONException {
        // given
        final JSONObject subscriber = new JSONObject()
            .put("subscriptions",
                    new JSONObject().put("other_product",
                            new JSONObject().put("expires_date", "2025-01-01T00:00:00Z")
                                .put("billing_issues_detected_at", "2025-01-02T00:00:00Z")))
            .put("entitlements",
                    new JSONObject().put("other_entitlement",
                            new JSONObject().put("expires_date", "2025-01-01T00:00:00Z")
                                .put("grace_period_expires_date", "2025-01-03T00:00:00Z")
                                .put("unsubscribe_detected_at", "2025-01-02T00:00:00Z")));

        // when
        final JSONObject result = subscriber(grant(body(subscriber)));

        // then
        final JSONObject otherSubscription = result.getJSONObject("subscriptions").getJSONObject("other_product");
        assertEquals(EXPIRES_DATE, otherSubscription.getString("expires_date"));
        assertFalse(otherSubscription.has("billing_issues_detected_at"));
        final JSONObject otherEntitlement = result.getJSONObject("entitlements").getJSONObject("other_entitlement");
        assertEquals(EXPIRES_DATE, otherEntitlement.getString("expires_date"));
        assertEquals(EXPIRES_DATE, otherEntitlement.getString("grace_period_expires_date"));
        assertFalse(otherEntitlement.has("unsubscribe_detected_at"));
        assertTrue(result.getJSONObject("subscriptions").has(PRODUCT));
        assertTrue(result.getJSONObject("entitlements").has(ENTITLEMENT));
    }

    @Test
    public void replacesTheNamedEntitlementWithAFreshGrant() throws JSONException {
        // given
        final JSONObject subscriber = new JSONObject().put("entitlements",
                new JSONObject().put(ENTITLEMENT,
                        new JSONObject().put("product_identifier", "free_trial")
                            .put("purchase_date", "2026-01-01T00:00:00Z")
                            .put("expires_date", "2026-01-08T00:00:00Z")
                            .put("billing_issues_detected_at", "2026-01-09T00:00:00Z")));

        // when
        final JSONObject entitlement = subscriber(grant(body(subscriber))).getJSONObject("entitlements")
            .getJSONObject(ENTITLEMENT);

        // then
        assertEquals(PRODUCT, entitlement.getString("product_identifier"));
        assertEquals(PURCHASE_DATE, entitlement.getString("purchase_date"));
        assertEquals(EXPIRES_DATE, entitlement.getString("expires_date"));
        assertFalse(entitlement.has("billing_issues_detected_at"));
    }

    @Test
    public void skipsEntriesThatAreNotObjects() throws JSONException {
        // given
        final JSONObject subscriber = new JSONObject()
            .put("subscriptions", new JSONObject().put("broken", "text").put("list", new JSONArray().put(1)))
            .put("entitlements", new JSONObject().put("broken", JSONObject.NULL));

        // when
        final JSONObject result = subscriber(grant(body(subscriber)));

        // then
        assertEquals("text", result.getJSONObject("subscriptions").getString("broken"));
        assertEquals(1, result.getJSONObject("subscriptions").getJSONArray("list").length());
        assertTrue(result.getJSONObject("subscriptions").has(PRODUCT));
        assertTrue(result.getJSONObject("entitlements").has(ENTITLEMENT));
    }

    @Test
    public void replacesSectionsThatAreNotObjects() throws JSONException {
        // given
        final JSONObject subscriber = new JSONObject().put("subscriptions", "none").put("entitlements", 3);

        // when
        final JSONObject result = subscriber(grant(body(subscriber)));

        // then
        assertTrue(result.getJSONObject("subscriptions").has(PRODUCT));
        assertTrue(result.getJSONObject("entitlements").has(ENTITLEMENT));
    }

    @Test
    public void preservesUnrelatedFields() throws JSONException {
        // given
        final JSONObject subscriber = new JSONObject().put("original_app_user_id", "$RCAnonymousID:abc")
            .put("non_subscriptions", new JSONObject());

        // when
        final JSONObject body = grant(body(subscriber));

        // then
        assertEquals("2026-01-01T00:00:00Z", body.getString("request_date"));
        assertEquals("$RCAnonymousID:abc", subscriber(body).getString("original_app_user_id"));
        assertTrue(subscriber(body).has("non_subscriptions"));
    }

    @Test
    public void grantingTwiceChangesNothingTheSecondTime() throws JSONException {
        // given
        final JSONObject body = grant(body(new JSONObject()));
        final String once = body.toString();

        // when
        final JSONObject granted = grant(body);

        // then
        assertEquals(once, granted.toString());
    }

    @Test
    public void grantsEachEntitlementIndependently() throws JSONException {
        // given
        final JSONObject body = body(new JSONObject());
        RevenueCatUnlock.grantEntitlement(body, "pro", "pro_yearly");

        // when
        RevenueCatUnlock.grantEntitlement(body, "plus", "plus_monthly");

        // then
        final JSONObject subscriber = subscriber(body);
        assertEquals("pro_yearly",
                subscriber.getJSONObject("entitlements").getJSONObject("pro").getString("product_identifier"));
        assertEquals("plus_monthly",
                subscriber.getJSONObject("entitlements").getJSONObject("plus").getString("product_identifier"));
        assertTrue(subscriber.getJSONObject("subscriptions").has("pro_yearly"));
        assertTrue(subscriber.getJSONObject("subscriptions").has("plus_monthly"));
    }

    @Test
    public void grantsALifetimeEntitlementWithoutAnExpiry() throws JSONException {
        // given
        final JSONObject body = body(new JSONObject());

        // when
        RevenueCatUnlock.grantLifetimeEntitlement(body, ENTITLEMENT, "pro_lifetime");

        // then
        final JSONObject subscriber = subscriber(body);
        final JSONObject entitlement = subscriber.getJSONObject("entitlements").getJSONObject(ENTITLEMENT);
        assertEquals("pro_lifetime", entitlement.getString("product_identifier"));
        assertEquals(PURCHASE_DATE, entitlement.getString("purchase_date"));
        assertTrue(entitlement.isNull("expires_date"));
    }

    @Test
    public void recordsALifetimePurchaseAsANonSubscription() throws JSONException {
        // given
        final JSONObject body = body(new JSONObject());

        // when
        RevenueCatUnlock.grantLifetimeEntitlement(body, ENTITLEMENT, "pro_lifetime");

        // then
        final JSONObject transaction = subscriber(body).getJSONObject("non_subscriptions")
            .getJSONArray("pro_lifetime")
            .getJSONObject(0);
        assertEquals(PURCHASE_DATE, transaction.getString("purchase_date"));
        assertEquals(PURCHASE_DATE, transaction.getString("original_purchase_date"));
        assertEquals("play_store", transaction.getString("store"));
        assertFalse(transaction.getBoolean("is_sandbox"));
        assertFalse(subscriber(body).has("subscriptions"));
    }

    @Test
    public void keepsAnExistingLifetimePurchase() throws JSONException {
        // given
        final JSONObject owned = new JSONObject().put("id", "owned");
        final JSONObject subscriber = new JSONObject().put("non_subscriptions",
                new JSONObject().put("pro_lifetime", new JSONArray().put(owned)));
        final JSONObject body = body(subscriber);

        // when
        RevenueCatUnlock.grantLifetimeEntitlement(body, ENTITLEMENT, "pro_lifetime");

        // then
        assertEquals("owned",
                subscriber(body).getJSONObject("non_subscriptions")
                    .getJSONArray("pro_lifetime")
                    .getJSONObject(0)
                    .getString("id"));
    }

    @Test
    public void ignoresNullBodyForALifetimeGrant() {
        RevenueCatUnlock.grantLifetimeEntitlement(null, ENTITLEMENT, "pro_lifetime");
    }

}

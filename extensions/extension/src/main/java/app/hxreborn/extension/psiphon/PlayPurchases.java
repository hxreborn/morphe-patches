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
package app.hxreborn.extension.psiphon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class PlayPurchases {

    private static final String TAG = "PlayPurchases";

    private static final String PURCHASE_CLASS = "com.android.billingclient.api.Purchase";

    private static final String PACKAGE_NAME = "com.psiphon3.subscription";

    private static final String PRODUCT_ID = "psiphon4_yearly";

    private static final long PURCHASED_AGO_MILLIS = 24L * 60 * 60 * 1000;

    private static final long PURCHASE_TIME_MILLIS = System.currentTimeMillis() - PURCHASED_AGO_MILLIS;

    private static final String PURCHASE_TOKEN = UUID.randomUUID().toString();

    private PlayPurchases() {

    }

    public static List<Object> withSyntheticSubscription(List<Object> purchases) {
        try {
            Object purchase = Class.forName(PURCHASE_CLASS)
                .getConstructor(String.class, String.class)
                .newInstance(purchaseJson(), "");
            List<Object> result = new ArrayList<>();
            if (purchases != null) {
                result.addAll(purchases);
            }
            result.add(purchase);
            return result;
        } catch (Exception ex) {
            Log.w(TAG, "Could not add the synthetic subscription purchase", ex);
            return purchases;
        }
    }

    private static String purchaseJson() throws Exception {
        return new JSONObject().put("orderId", "GPA.3370-0000-0000-00000")
            .put("packageName", PACKAGE_NAME)
            .put("productId", PRODUCT_ID)
            .put("productIds", new JSONArray().put(PRODUCT_ID))
            .put("purchaseTime", PURCHASE_TIME_MILLIS)
            .put("purchaseState", 0)
            .put("purchaseToken", PURCHASE_TOKEN)
            .put("quantity", 1)
            .put("autoRenewing", true)
            .put("acknowledged", true)
            .toString();
    }

}

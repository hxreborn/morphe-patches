/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.cubasisle3;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public final class StoreUnlock {

    private static final List<String> PRODUCT_IDS = Arrays.asList("91538", "91539", "91540", "91541", "91542", "91543",
            "91865", "91866", "91867", "92175", "92404", "92406", "92636", "AMP");

    private StoreUnlock() {

    }

    public static void reportPurchasedProducts(Object activity) {
        try {
            Method shopPurchasedProducts = activity.getClass()
                .getDeclaredMethod("shopPurchasedProducts", List.class, boolean.class);
            shopPurchasedProducts.setAccessible(true);
            shopPurchasedProducts.invoke(activity, PRODUCT_IDS, false);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

}

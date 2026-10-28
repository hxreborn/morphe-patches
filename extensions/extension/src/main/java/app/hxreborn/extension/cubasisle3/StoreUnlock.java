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

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
package app.hxreborn.extension.aliexpress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class SponsoredProducts {

    private static final String LIST_ITEMS = "listItems";

    private static final String CELLS = "cells";

    private static final String PRODUCT_ID = "productId";

    private static final String SPONSORSHIP = "p4p";

    private SponsoredProducts() {
    }

    public static void filterIfEnabled(Map<String, Object> source, String key) {
        if (PatchSettings.removeSponsoredItems()) {
            filter(source, key);
        }
    }

    static void filter(Map<String, Object> source, String key) {
        if (!LIST_ITEMS.equals(key) && !CELLS.equals(key)) {
            return;
        }
        Object value = source.get(key);
        if (!(value instanceof List<?>)) {
            return;
        }
        List<?> products = (List<?>) value;
        if (!containsSponsoredProduct(products)) {
            return;
        }
        List<Object> filtered = new ArrayList<>(products.size());
        for (Object item : products) {
            if (isSponsoredProduct(item)) {
                continue;
            }
            filtered.add(item);
        }
        source.put(key, filtered);
    }

    private static boolean containsSponsoredProduct(List<?> products) {
        for (Object product : products) {
            if (isSponsoredProduct(product)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSponsoredProduct(Object item) {
        if (!(item instanceof Map<?, ?>)) {
            return false;
        }
        Map<?, ?> product = (Map<?, ?>) item;
        return product.get(PRODUCT_ID) != null && product.get(SPONSORSHIP) instanceof Map<?, ?>;
    }

}

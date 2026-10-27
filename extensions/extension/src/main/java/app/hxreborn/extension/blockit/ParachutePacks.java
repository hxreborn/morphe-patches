/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.blockit;

@SuppressWarnings("unused")
public final class ParachutePacks {

    private static final String PACK_PREFIX = "parachute_x";

    private ParachutePacks() {

    }

    public static int size(String productId) {
        if (productId == null || !productId.startsWith(PACK_PREFIX)) {
            return 0;
        }
        try {
            return Integer.parseInt(productId.substring(PACK_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}

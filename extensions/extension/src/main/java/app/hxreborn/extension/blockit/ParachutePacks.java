/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

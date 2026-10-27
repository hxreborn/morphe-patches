/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.promos;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class Promotions {

    private Promotions() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean removed() {
        return PatchSettings.REMOVE_PROMOTIONS.get();
    }

    public static boolean shown(boolean original) {
        return original && !removed();
    }

    public static boolean dismissed(boolean original) {
        return original || removed();
    }

}

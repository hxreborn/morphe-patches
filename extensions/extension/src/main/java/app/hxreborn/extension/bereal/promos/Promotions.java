/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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

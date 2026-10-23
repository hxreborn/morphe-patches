/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.seen;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class ViewReceipts {

    private ViewReceipts() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean skipped() {
        return PatchSettings.DISABLE_VIEW_RECEIPTS.get();
    }

}

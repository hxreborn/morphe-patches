/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.bereal.download;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class FriendsPostDownload {

    private FriendsPostDownload() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean enabled() {
        return PatchSettings.SAVE_FRIENDS_BEREALS.get();
    }

}

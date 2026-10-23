/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

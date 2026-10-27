/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.blur;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class FriendsPosts {

    private FriendsPosts() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean blurred(boolean original) {
        return blurred(original, PatchSettings.SHOW_FRIENDS_POSTS.get());
    }

    static boolean blurred(boolean original, boolean show) {
        return original && !show;
    }

}

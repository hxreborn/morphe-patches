/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.bereal;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class FriendSuggestions {

    private FriendSuggestions() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean removed() {
        return PatchSettings.REMOVE_FRIEND_SUGGESTIONS.get();
    }

    public static List<?> suggestions(List<?> original) {
        return removed() ? Collections.emptyList() : original;
    }

    public static boolean shouldDisplay(boolean original) {
        return original && !removed();
    }

}

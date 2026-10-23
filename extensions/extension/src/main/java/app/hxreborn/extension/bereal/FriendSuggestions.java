/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

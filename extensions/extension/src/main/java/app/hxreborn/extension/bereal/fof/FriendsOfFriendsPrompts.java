/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.fof;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class FriendsOfFriendsPrompts {

    private FriendsOfFriendsPrompts() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean removed() {
        return PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_PROMPTS.get();
    }

    public static boolean showFooter(boolean original) {
        return keepUnlessRemoved(original, removed());
    }

    static boolean keepUnlessRemoved(boolean original, boolean removed) {
        return original && !removed;
    }

}

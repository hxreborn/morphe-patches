/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.streak;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class StreakPrompts {

    private StreakPrompts() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean removed() {
        return PatchSettings.REMOVE_STREAK_PROMPTS.get();
    }

}

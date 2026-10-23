/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.typing;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class TypingIndicator {

    private TypingIndicator() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean shouldSend(final boolean typing) {
        return typing && !PatchSettings.DISABLE_TYPING_INDICATOR.get();
    }

}

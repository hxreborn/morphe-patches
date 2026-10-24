/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.latelabel;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class LateLabel {

    private LateLabel() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean isLate(boolean original) {
        return original && !PatchSettings.REMOVE_LATE_LABEL.get();
    }

    public static long lateBy(long original) {
        return PatchSettings.REMOVE_LATE_LABEL.get() ? 0L : original;
    }

    public static Integer lateSeconds(Integer original) {
        return PatchSettings.REMOVE_LATE_LABEL.get() ? null : original;
    }

}

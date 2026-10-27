/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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

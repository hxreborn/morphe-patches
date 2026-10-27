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

final class PatchSummary {

    private PatchSummary() {
    }

    static int countOn(int alwaysOn, boolean... switchStates) {
        int on = alwaysOn;
        for (boolean state : switchStates) {
            if (state) {
                on++;
            }
        }
        return on;
    }

    static String status(int on, int off, int applied) {
        return on + " on \u2022 " + off + " off \u2022 " + applied + " applied";
    }

}

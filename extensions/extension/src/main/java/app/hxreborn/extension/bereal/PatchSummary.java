/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

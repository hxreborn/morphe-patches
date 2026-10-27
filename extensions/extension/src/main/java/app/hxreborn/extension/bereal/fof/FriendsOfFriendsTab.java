/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.bereal.fof;

import java.util.List;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class FriendsOfFriendsTab {

    private FriendsOfFriendsTab() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean addTab(List<Object> tabs, Object tab) {
        return addUnlessRemoved(tabs, tab, PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB.get());
    }

    static boolean addUnlessRemoved(List<Object> tabs, Object tab, boolean removed) {
        return !removed && tabs.add(tab);
    }

}

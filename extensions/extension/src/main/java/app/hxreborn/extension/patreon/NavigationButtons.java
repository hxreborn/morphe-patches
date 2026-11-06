/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.patreon;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public final class NavigationButtons {

    static final String FEED = "MemberModeHomeTab";

    static final String LIBRARY = "MemberModeLibraryTab";

    static final String QUIP = "MemberModeCreateQuickPostTab";

    static final String COMMUNITY = "MemberModeChatTab";

    static final String NOTIFICATIONS = "MemberModeNotificationsTab";

    static final String[] TABS = { FEED, LIBRARY, QUIP, COMMUNITY, NOTIFICATIONS };

    static final String[] TAB_LABELS = { "Feed", "Library", "Quip", "Community", "Notifications" };

    static final String[] TAB_ICONS = { "hx_patreon_ic_feed", "hx_patreon_ic_library", "hx_patreon_ic_quip",
            "hx_patreon_ic_community", "hx_patreon_ic_notifications" };

    private static final String HIDE_TAB_KEY_PREFIX = "hide_navigation_tab_";

    private NavigationButtons() {
    }

    public static boolean isPatched() {
        return false;
    }

    public static String defaultHiddenTabs() {
        return "";
    }

    public static Object visibleTab(Object tab) {
        return (tab != null && isHidden(tab.toString())) ? null : tab;
    }

    public static boolean startsOnLibrary() {
        return isHidden(FEED) && !isHiddenByUser(LIBRARY);
    }

    public static boolean startsOnCommunity() {
        return isHidden(FEED) && isHiddenByUser(LIBRARY) && !isHiddenByUser(COMMUNITY);
    }

    static boolean isHidden(String tab) {
        if (FEED.equals(tab)) {
            return isHiddenByUser(FEED) && !(isHiddenByUser(LIBRARY) && isHiddenByUser(COMMUNITY));
        }
        return isHiddenByUser(tab);
    }

    static boolean isHiddenByUser(String tab) {
        return PatchSettings.isEnabled(HIDE_TAB_KEY_PREFIX + tab, defaultHidden().contains(tab));
    }

    static void setHiddenByUser(String tab, boolean hidden) {
        PatchSettings.setEnabled(HIDE_TAB_KEY_PREFIX + tab, hidden);
    }

    private static List<String> defaultHidden() {
        return Arrays.asList(defaultHiddenTabs().split(","));
    }

    public static String patchName() {
        return null;
    }

}

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
package app.hxreborn.extension.outlook.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.view.Menu;
import android.view.MenuItem;

import app.hxreborn.extension.outlook.settings.PatchSettings;

@SuppressWarnings("unused")
public final class NavigationTabs {

    private static final int NOT_FOUND = -1;

    private static final int MIN_PINNED_APPS_TO_HIDE_CALENDAR = 2;

    private static final Map<Object, Integer> CALENDAR_INDEX_BY_APP_MANAGER = Collections
        .synchronizedMap(new WeakHashMap<>());

    private static final Map<Object, Boolean> NAVIGATION_RAIL_BY_SUITE_STATE = Collections
        .synchronizedMap(new WeakHashMap<>());

    private NavigationTabs() {
    }

    public static List<Object> pinnedApps(Object appManager, List<Object> pinnedApps) {
        CALENDAR_INDEX_BY_APP_MANAGER.put(appManager, indexOfCalendar(pinnedApps));
        int calendarIndex = hiddenCalendarIndex(pinnedApps);
        if (calendarIndex == NOT_FOUND) {
            return pinnedApps;
        }
        List<Object> shownApps = new ArrayList<>(pinnedApps);
        shownApps.remove(calendarIndex);
        return shownApps;
    }

    public static List<Object> moreApps(List<Object> pinnedApps, List<Object> moreApps) {
        int calendarIndex = hiddenCalendarIndex(pinnedApps);
        if (calendarIndex == NOT_FOUND) {
            return moreApps;
        }
        List<Object> calendarFirst = new ArrayList<>(moreApps.size() + 1);
        calendarFirst.add(pinnedApps.get(calendarIndex));
        calendarFirst.addAll(moreApps);
        return calendarFirst;
    }

    public static int calendarRestoreIndex(Object appManager, List<Object> pinnedApps, List<Object> moreApps) {
        int storedIndex = storedCalendarIndex(appManager);
        if (!PatchSettings.hideCalendarTab() || storedIndex == NOT_FOUND || indexOfCalendar(pinnedApps) != NOT_FOUND
                || indexOfCalendar(moreApps) == NOT_FOUND) {
            return NOT_FOUND;
        }
        return Math.min(storedIndex, pinnedApps.size());
    }

    public static List<Object> savedPinnedApps(int restoreIndex, List<Object> pinnedApps, List<Object> moreApps) {
        if (restoreIndex == NOT_FOUND) {
            return pinnedApps;
        }
        List<Object> restoredApps = new ArrayList<>(pinnedApps);
        restoredApps.add(restoreIndex, moreApps.get(indexOfCalendar(moreApps)));
        return restoredApps;
    }

    public static List<Object> savedMoreApps(int restoreIndex, List<Object> moreApps) {
        if (restoreIndex == NOT_FOUND) {
            return moreApps;
        }
        List<Object> remainingApps = new ArrayList<>(moreApps);
        remainingApps.remove(indexOfCalendar(moreApps));
        return remainingApps;
    }

    public static void hideAppsTab(Menu tabs) {
        if (!PatchSettings.hideAppsTab()) {
            return;
        }
        int appsTabId = appsTabId();
        for (int index = 0; index < tabs.size(); index++) {
            MenuItem tab = tabs.getItem(index);
            if (tab.getItemId() == appsTabId) {
                tab.setVisible(false);
            }
        }
    }

    public static boolean isAppsTabHidden(boolean noMoreApps) {
        return noMoreApps || PatchSettings.hideAppsTab();
    }

    public static void setNavigationRail(Object navigationSuiteState, boolean navigationRail) {
        NAVIGATION_RAIL_BY_SUITE_STATE.put(navigationSuiteState, navigationRail);
    }

    public static boolean navigationSuiteVisible(Object navigationSuiteState, boolean visible) {
        return Boolean.TRUE.equals(NAVIGATION_RAIL_BY_SUITE_STATE.get(navigationSuiteState)) ? visible
                : navigationBarVisible(visible);
    }

    public static boolean navigationBarVisible(boolean visible) {
        return visible && !PatchSettings.hideNavigationBar();
    }

    private static int hiddenCalendarIndex(List<Object> pinnedApps) {
        if (!PatchSettings.hideCalendarTab() || pinnedApps.size() < MIN_PINNED_APPS_TO_HIDE_CALENDAR) {
            return NOT_FOUND;
        }
        return indexOfCalendar(pinnedApps);
    }

    private static int storedCalendarIndex(Object appManager) {
        Integer calendarIndex = CALENDAR_INDEX_BY_APP_MANAGER.get(appManager);
        return (calendarIndex != null) ? calendarIndex : NOT_FOUND;
    }

    private static int indexOfCalendar(List<Object> apps) {
        for (int index = 0; index < apps.size(); index++) {
            if (isCalendar(apps.get(index))) {
                return index;
            }
        }
        return NOT_FOUND;
    }

    private static boolean isCalendar(Object app) {
        return false;
    }

    private static int appsTabId() {
        return 0;
    }

}

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
package app.hxreborn.extension.botim;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public final class MainTabsTest {

    private static final Object HOME = "home";

    private static final Object CALLS = "calls";

    @Test
    public void removesTheTabAndKeepsTheOrderOfTheRest() {
        Map<Object, Object> tabs = new LinkedHashMap<>();
        tabs.put(0, HOME);
        tabs.put(1, CALLS);
        tabs.put(2, "chats");

        Map<Object, Object> visible = MainTabs.withoutTab(tabs, HOME);

        assertEquals(Arrays.asList(CALLS, "chats"), Arrays.asList(visible.values().toArray()));
    }

    @Test
    public void keepsTabsWhenTheTabIsMissing() {
        Map<Object, Object> tabs = new LinkedHashMap<>();
        tabs.put(1, CALLS);

        assertEquals(1, MainTabs.withoutTab(tabs, HOME).size());
    }

}

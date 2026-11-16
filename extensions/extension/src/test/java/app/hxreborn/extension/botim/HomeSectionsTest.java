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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public final class HomeSectionsTest {

    private static Map<String, Object> item(String id) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        return item;
    }

    @Test
    public void removesServicesAndDiscoverItems() {
        Map<String, Object> topBar = item("host.home.top_bar");
        Map<String, Object> todo = item("host.todo.list");
        List<Object> items = Arrays.asList(topBar, item("services.root"), todo, item("discover.root"));

        assertEquals(Arrays.asList(topBar, todo), HomeSections.withoutServicesAndDiscover(items));
    }

    @Test
    public void keepsItemsThatAreNotMaps() {
        List<Object> items = Arrays.asList("services.root", 1);

        assertEquals(items, HomeSections.withoutServicesAndDiscover(items));
    }

    @Test
    public void keepsItemsWithoutId() {
        List<Object> items = Collections.singletonList(new HashMap<String, Object>());

        assertEquals(items, HomeSections.withoutServicesAndDiscover(items));
    }

}

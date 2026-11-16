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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class HomeSections {

    private static final String SERVICES_ITEM_ID = "services.root";

    private static final String DISCOVER_ITEM_ID = "discover.root";

    private static final String ITEM_ID_KEY = "id";

    private HomeSections() {

    }

    public static List<Object> withoutServicesAndDiscover(List<Object> items) {
        List<Object> visible = new ArrayList<>(items.size());
        for (Object item : items) {
            if (item instanceof Map && isRemoved(((Map<?, ?>) item).get(ITEM_ID_KEY))) {
                continue;
            }
            visible.add(item);
        }
        return visible;
    }

    private static boolean isRemoved(Object id) {
        return SERVICES_ITEM_ID.equals(id) || DISCOVER_ITEM_ID.equals(id);
    }

}

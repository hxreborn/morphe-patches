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
package app.hxreborn.extension.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ColorEntries {

    private final List<Integer> resourceIds = new ArrayList<>();

    private final List<String> names = new ArrayList<>();

    private final List<Integer> colors = new ArrayList<>();

    public void add(int resourceId, String name, int color) {
        this.resourceIds.add(resourceId);
        this.names.add(name);
        this.colors.add(color);
    }

    int size() {
        return this.resourceIds.size();
    }

    int resourceId(int position) {
        return this.resourceIds.get(position);
    }

    int color(int position) {
        return this.colors.get(position);
    }

    List<String> names() {
        return Collections.unmodifiableList(this.names);
    }

}

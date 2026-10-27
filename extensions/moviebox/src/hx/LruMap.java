/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import java.util.LinkedHashMap;
import java.util.Map;

final class LruMap<V> extends LinkedHashMap<String, V> {

    private static final long serialVersionUID = 1L;

    private static final float LOAD_FACTOR = 0.75f;

    private final int capacity;

    LruMap(int capacity) {
        super(capacity, LOAD_FACTOR, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
        return size() > this.capacity;
    }

}

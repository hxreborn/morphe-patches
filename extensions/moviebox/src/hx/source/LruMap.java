/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.source;

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

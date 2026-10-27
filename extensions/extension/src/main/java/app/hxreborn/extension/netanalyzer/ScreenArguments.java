/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.netanalyzer;

import java.util.LinkedHashMap;
import java.util.Map;

final class ScreenArguments {

    private static final int LIMIT = 16;

    private static final Map<Integer, Object> ENTRIES = new LinkedHashMap<Integer, Object>() {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Object> eldest) {
            return size() > LIMIT;
        }

    };

    private static int next;

    private ScreenArguments() {
    }

    static synchronized int put(Object value) {
        next++;
        ENTRIES.put(next, value);
        return next;
    }

    static synchronized Object get(int key) {
        return ENTRIES.get(key);
    }

}

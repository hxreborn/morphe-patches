/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LruMapTest {

    @Test
    public void dropsTheEldestEntryBeyondItsCapacity() {
        // given
        LruMap<String> map = new LruMap<>(2);
        map.put("a", "1");
        map.put("b", "2");

        // when
        map.put("c", "3");

        // then
        assertNull(map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));
    }

    @Test
    public void keepsTheEntryThatWasReadLast() {
        // given
        LruMap<String> map = new LruMap<>(2);
        map.put("a", "1");
        map.put("b", "2");
        map.get("a");

        // when
        map.put("c", "3");

        // then
        assertEquals("1", map.get("a"));
        assertFalse(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
    }

}

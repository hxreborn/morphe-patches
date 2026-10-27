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

import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(map.get("a")).isNull();
        Assertions.assertThat(map.get("b")).isEqualTo("2");
        Assertions.assertThat(map.get("c")).isEqualTo("3");
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
        Assertions.assertThat(map.get("a")).isEqualTo("1");
        Assertions.assertThat(map.containsKey("b")).isFalse();
        Assertions.assertThat(map.containsKey("c")).isTrue();
    }

}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.castle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DeviceGuidTest {

    @Test
    public void createsA36CharacterHexGuid() {
        // when
        String guid = DeviceGuid.newGuid();

        // then
        assertTrue(guid.matches("[0-9a-f]{36}"));
    }

    @Test
    public void createsADifferentGuidEachTime() {
        // when
        String first = DeviceGuid.newGuid();
        String second = DeviceGuid.newGuid();

        // then
        assertNotEquals(first, second);
    }

    @Test
    public void asksNoRetryWhenTheResponseIsNotBlacklisted() {
        // given
        DeviceGuid.observeResponse("{\"inBlacklist\":false}");

        // when
        boolean retry = DeviceGuid.shouldRetry();

        // then
        assertFalse(retry);
    }

    @Test
    public void asksOneRetryWithANewGuidWhenTheResponseIsBlacklisted() {
        // given
        DeviceGuid.observeResponse("{\"inBlacklist\":true}");

        // when
        boolean retry = DeviceGuid.shouldRetry();

        // then
        assertTrue(retry);
        assertTrue(DeviceGuid.switchedGuid().matches("[0-9a-f]{36}"));
        assertFalse(DeviceGuid.shouldRetry());
    }

    @Test
    public void keepsTheGuidWhenTheRetryIsBlacklistedAgain() {
        // given
        DeviceGuid.observeResponse("{\"inBlacklist\":true}");
        DeviceGuid.shouldRetry();
        String switched = DeviceGuid.switchedGuid();

        // when
        DeviceGuid.observeResponse("{\"inBlacklist\":true}");

        // then
        assertFalse(DeviceGuid.shouldRetry());
        assertEquals(switched, DeviceGuid.switchedGuid());
    }

}

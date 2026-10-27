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
package app.hxreborn.extension.serializd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public final class AdFlagsTest {

    @Test
    public void turnsOffEveryAdFlag() {
        // given
        String body = "{\"username\": \"a\", \"shouldShowAds\": true, \"shouldShowStickyNavigationAds\": true, "
                + "\"shouldUseSingletonMrec\":true}";

        // when
        String sanitized = AdFlags.disable(body);

        // then
        assertEquals("{\"username\": \"a\", \"shouldShowAds\": false, \"shouldShowStickyNavigationAds\": false, "
                + "\"shouldUseSingletonMrec\":false}", sanitized);
    }

    @Test
    public void keepsABodyWithoutAdFlags() {
        // given
        String body = "{\"shouldShowDiscussionsFirst\": true}";

        // when
        String sanitized = AdFlags.disable(body);

        // then
        assertSame(body, sanitized);
    }

    @Test
    public void keepsADisabledFlag() {
        // given
        String body = "{\"shouldShowAds\": false}";

        // when
        String sanitized = AdFlags.disable(body);

        // then
        assertEquals(body, sanitized);
    }

    @Test
    public void leavesOtherShouldFlagsEnabled() {
        // given
        String body = "{\"shouldShowAds\": true, \"shouldShowDiscussionsFirst\": true}";

        // when
        String sanitized = AdFlags.disable(body);

        // then
        assertEquals("{\"shouldShowAds\": false, \"shouldShowDiscussionsFirst\": true}", sanitized);
    }

    @Test
    public void keepsAFlagHeldAsAString() {
        // given
        String body = "{\"shouldShowAds\": \"true\"}";

        // when
        String sanitized = AdFlags.disable(body);

        // then
        assertEquals(body, sanitized);
    }

    @Test
    public void passesNullThrough() {
        // when
        String sanitized = AdFlags.disable(null);

        // then
        assertNull(sanitized);
    }

}

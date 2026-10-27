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
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PatchSummaryTest {

    @Test
    public void reportsOnOffAndAppliedCounts() {
        // when
        String status = PatchSummary.status(12, 5, 17);

        // then
        assertEquals("12 on • 5 off • 17 applied", status);
    }

    @Test
    public void countsAlwaysOnRowsAsOn() {
        // when
        int on = PatchSummary.countOn(3, true, false, true);

        // then
        assertEquals(5, on);
    }

    @Test
    public void countsNothingOnWhenEverySwitchIsOff() {
        // when
        int on = PatchSummary.countOn(0, false, false);

        // then
        assertEquals(0, on);
    }

}

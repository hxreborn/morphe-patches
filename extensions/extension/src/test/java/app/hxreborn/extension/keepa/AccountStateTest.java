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
package app.hxreborn.extension.keepa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class AccountStateTest {

    @Test
    public void wireValuesAreStable() {
        assertEquals("ok", AccountState.OK.wireValue);
        assertEquals("throttled", AccountState.THROTTLED.wireValue);
        assertEquals("invalid", AccountState.INVALID.wireValue);
    }

    @Test
    public void everyStateRoundTripsThroughItsWireValue() {
        for (AccountState state : AccountState.values()) {
            assertEquals(state, AccountState.fromWireValue(state.wireValue));
        }
    }

    @Test
    public void wireValuesAreDistinct() {
        final AccountState[] states = AccountState.values();
        for (int i = 0; i < states.length; i++) {
            for (int j = i + 1; j < states.length; j++) {
                assertNotEquals(states[i].wireValue, states[j].wireValue);
            }
        }
    }

    @Test
    public void unknownMissingOrDifferentlyCasedValuesReadAsInvalid() {
        for (String value : new String[] { null, "", "OK", "Throttled", "banned", " ok", "ok " }) {
            assertEquals(String.valueOf(value), AccountState.INVALID, AccountState.fromWireValue(value));
        }
    }

}

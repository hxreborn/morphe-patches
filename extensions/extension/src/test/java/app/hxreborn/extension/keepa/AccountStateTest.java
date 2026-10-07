/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

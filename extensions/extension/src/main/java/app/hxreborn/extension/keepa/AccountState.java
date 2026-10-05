/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

enum AccountState {

    OK("ok"), THROTTLED("throttled"), INVALID("invalid");

    final String wireValue;

    AccountState(String wireValue) {
        this.wireValue = wireValue;
    }

    static AccountState fromWireValue(String value) {
        for (AccountState state : values()) {
            if (state.wireValue.equals(value)) {
                return state;
            }
        }
        return INVALID;
    }

}

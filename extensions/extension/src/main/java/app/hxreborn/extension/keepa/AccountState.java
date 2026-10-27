/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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

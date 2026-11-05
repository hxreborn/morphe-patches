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
package app.hxreborn.extension.viber;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;

@SuppressWarnings({ "unused", "deprecation" })
public final class ChooseAccountIntent {

    private static final String EXTRA_SELECTED_ACCOUNT = "selectedAccount";

    private static final String EXTRA_ALLOWABLE_ACCOUNT_TYPES = "allowableAccountTypes";

    private ChooseAccountIntent() {
    }

    public static Intent fromGmsPickerIntent(Intent gmsPickerIntent) {
        Account selectedAccount = gmsPickerIntent.getParcelableExtra(EXTRA_SELECTED_ACCOUNT);
        String[] accountTypes = gmsPickerIntent.getStringArrayExtra(EXTRA_ALLOWABLE_ACCOUNT_TYPES);
        return AccountManager.newChooseAccountIntent(selectedAccount, null, accountTypes, null, null, null, null);
    }

}

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

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

@SuppressWarnings({ "unused", "deprecation" })
public final class GmsCoreAccountRequest {

    private static final Uri ACCOUNTS_URI = Uri.parse("content://com.google.android.gms.auth.accounts");

    private static final String METHOD_GET_ACCOUNTS = "get_accounts";

    private static final String EXTRA_ACCOUNTS = "accounts";

    private static final String ACTIVITY_THREAD_CLASS = "android.app.ActivityThread";

    private static final String CURRENT_APPLICATION_METHOD = "currentApplication";

    private GmsCoreAccountRequest() {
    }

    public static Account[] getAccountsByType(AccountManager manager, String type) {
        Account[] visibleAccounts = manager.getAccountsByType(type);
        if (visibleAccounts.length > 0) {
            return visibleAccounts;
        }
        return requestAccounts(type);
    }

    private static Account[] requestAccounts(String type) {
        Context context = currentApplication();
        if (context == null) {
            return new Account[0];
        }
        Bundle result;
        try {
            result = context.getContentResolver().call(ACCOUNTS_URI, METHOD_GET_ACCOUNTS, type, null);
        } catch (SecurityException | IllegalArgumentException ex) {
            Log.w(PatchSettings.LOG_TAG, "GmsCore account list request failed", ex);
            return new Account[0];
        }
        Parcelable[] parcelables = (result != null) ? result.getParcelableArray(EXTRA_ACCOUNTS) : null;
        if (parcelables == null) {
            return new Account[0];
        }
        List<Account> accounts = new ArrayList<>();
        for (Parcelable parcelable : parcelables) {
            if (parcelable instanceof Account) {
                accounts.add((Account) parcelable);
            }
        }
        return accounts.toArray(new Account[0]);
    }

    private static Application currentApplication() {
        try {
            return (Application) Class.forName(ACTIVITY_THREAD_CLASS)
                .getMethod(CURRENT_APPLICATION_METHOD)
                .invoke(null);
        } catch (ReflectiveOperationException ex) {
            Log.w(PatchSettings.LOG_TAG, "Application lookup failed", ex);
            return null;
        }
    }

}

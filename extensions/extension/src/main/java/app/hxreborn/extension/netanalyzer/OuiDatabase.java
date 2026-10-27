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
package app.hxreborn.extension.netanalyzer;

import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;

final class OuiDatabase {

    private static final String FILE = "oui_database.db";

    private static final long LOCALLY_ADMINISTERED_PREFIX = 0x020000;

    private static final int MAC_TEXT_LENGTH = 17;

    private static final int OUI_HEX_DIGITS = 6;

    private static final int HEX_RADIX = 16;

    private OuiDatabase() {
    }

    static String vendor(Context context, String mac) {
        if (mac == null || mac.length() < MAC_TEXT_LENGTH) {
            return null;
        }
        final long prefix;
        try {
            final String digits = mac.replace(":", "").toUpperCase(Locale.ROOT);
            prefix = Long.parseLong(digits.substring(0, OUI_HEX_DIGITS), HEX_RADIX);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (prefix == LOCALLY_ADMINISTERED_PREFIX) {
            return Build.MANUFACTURER;
        }
        final File file = new File(context.getFilesDir(), FILE);
        if (!file.exists()) {
            return null;
        }
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                SQLiteStatement query = database.compileStatement("SELECT vendor FROM oui WHERE mac=?")) {
            query.bindLong(1, prefix);
            return query.simpleQueryForString();
        } catch (SQLiteDoneException ex) {
            return null;
        } catch (SQLiteException ex) {
            return null;
        }
    }

}

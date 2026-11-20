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
package app.hxreborn.extension.terabox;

import java.text.NumberFormat;
import java.util.Map;
import java.util.WeakHashMap;

import android.database.Cursor;
import android.util.Log;

@SuppressWarnings("unused")
public final class BackupCount {

    private static final String ACTIVE_SELECTION = "state = 104 OR state = 100";

    private static final String PAUSED_SELECTION = " OR state = 105";

    private static final Map<Object, Integer> TITLE_COUNTS = new WeakHashMap<>();

    private BackupCount() {
    }

    public static String selection() {
        return PatchSettings.isBackupCountShown() ? ACTIVE_SELECTION + PAUSED_SELECTION : ACTIVE_SELECTION;
    }

    public static int withRemainingCount(int current) {
        if (!PatchSettings.isBackupCountShown()) {
            return current;
        }
        try (Cursor cursor = queryPendingFiles()) {
            return (cursor != null) ? cursor.getCount() : current;
        } catch (RuntimeException ex) {
            Log.w(PatchSettings.LOG_TAG, "Failed to read remaining backup files", ex);
            return current;
        }
    }

    private static Cursor queryPendingFiles() {
        return null;
    }

    public static boolean shouldRefreshTitle(Object owner, int remaining) {
        if (!PatchSettings.isBackupCountShown()) {
            TITLE_COUNTS.remove(owner);
            return false;
        }
        Integer previous = TITLE_COUNTS.put(owner, remaining);
        return previous == null || previous != remaining;
    }

    public static String withRemaining(String title, int remaining) {
        if (!PatchSettings.isBackupCountShown() || title == null || remaining <= 0) {
            return title;
        }
        return title + " (" + NumberFormat.getIntegerInstance().format(remaining) + " left)";
    }

}

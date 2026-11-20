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

@SuppressWarnings("unused")
public final class OriginalDateSort {

    private static final String UPLOAD_TIME_ASCENDING = "isdir DESC, server_mtime ASC";

    private static final String UPLOAD_TIME_DESCENDING = "isdir DESC, server_mtime DESC";

    private static final String ORIGINAL_TIME = "CASE WHEN client_mtime > 0 THEN client_mtime ELSE server_mtime END";

    private static final String ORIGINAL_TIME_ASCENDING = "isdir DESC, " + ORIGINAL_TIME + " ASC";

    private static final String ORIGINAL_TIME_DESCENDING = "isdir DESC, " + ORIGINAL_TIME + " DESC";

    private static volatile boolean processSetting;

    private OriginalDateSort() {
    }

    public static void loadProcessSetting() {
        processSetting = PatchSettings.isOriginalDateSorted();
    }

    public static boolean sortsByOriginalDate() {
        return processSetting;
    }

    public static String timeSortOrder(String order) {
        if (!processSetting) {
            return order;
        }
        if (UPLOAD_TIME_ASCENDING.equals(order)) {
            return ORIGINAL_TIME_ASCENDING;
        }
        if (UPLOAD_TIME_DESCENDING.equals(order)) {
            return ORIGINAL_TIME_DESCENDING;
        }
        return order;
    }

    public static boolean usesOriginalTime(long originalTime) {
        return processSetting && originalTime > 0;
    }

}

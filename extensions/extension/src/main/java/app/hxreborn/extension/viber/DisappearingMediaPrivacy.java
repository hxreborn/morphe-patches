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

import android.content.ContentValues;

@SuppressWarnings("unused")
public final class DisappearingMediaPrivacy {

    private static final int SCREENSHOT_EVENT = 2;

    private static final long DISAPPEARING_MESSAGE_FLAG = 1L << 27;

    private static final String DISAPPEARING_MESSAGES = "(timebomb>0 OR (extra_flags & " + DISAPPEARING_MESSAGE_FLAG
            + ")<>0)";

    private static final String SYNC_READ = "sync_read";

    private DisappearingMediaPrivacy() {
    }

    static int update(Object database, String table, ContentValues values, String selection, String[] arguments) {
        return 0;
    }

    public static boolean blockScreenshot(int eventType) {
        return eventType == SCREENSHOT_EVENT && PatchSettings.disappearingMediaPrivacy();
    }

    public static void updateInferredReadTime(Object store, long conversation, long token, long timestamp,
            boolean incoming) {
    }

    public static String inferredReadSelection(String selection) {
        return (PatchSettings.disappearingMediaPrivacy()) ? "(" + selection + ") AND NOT " + DISAPPEARING_MESSAGES
                : selection;
    }

    public static int updateReadState(Object database, String table, ContentValues values, String selection,
            String[] arguments) {
        if (!PatchSettings.disappearingMediaPrivacy()) {
            return update(database, table, values, selection, arguments);
        }
        ContentValues disappearingValues = new ContentValues(values);
        disappearingValues.put(SYNC_READ, 0);
        int disappearingRows = update(database, table, disappearingValues,
                "(" + selection + ") AND " + DISAPPEARING_MESSAGES, arguments);
        return disappearingRows
                + update(database, table, values, "(" + selection + ") AND NOT " + DISAPPEARING_MESSAGES, arguments);
    }

}

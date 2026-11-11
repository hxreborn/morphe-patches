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
package app.hxreborn.extension.readera;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.ScrollView;

@SuppressWarnings("unused")
public final class SelectionSearch {

    static final String TITLE = "Search in book";

    private static final long PENDING_QUERY_TIMEOUT_MS = 5000;

    private static String pendingQuery;

    private static long pendingSince;

    private SelectionSearch() {
    }

    public static void attach(SelectionToolbar toolbar, ScrollView selectionMenu, ScrollView quoteMenu) {
        SelectionMenu.addAction(selectionMenu, quoteMenu, TITLE, () -> search(toolbar));
    }

    public static void onTextSearchShown(Object dialog) {
        String query = pendingQuery;
        pendingQuery = null;
        if (query == null || SystemClock.uptimeMillis() - pendingSince > PENDING_QUERY_TIMEOUT_MS) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> submitTextSearch(dialog, query));
    }

    static String searchQuery(String selection) {
        return selection.replaceAll("\\s+", " ").trim();
    }

    private static void search(SelectionToolbar toolbar) {
        String query = searchQuery(toolbar.selectionText(false));
        if (query.isEmpty()) {
            return;
        }
        Activity activity = toolbar.selectionActivity();
        toolbar.closeSelection();
        pendingQuery = query;
        pendingSince = SystemClock.uptimeMillis();
        openTextSearch(activity);
    }

    static void openTextSearch(Activity activity) {
    }

    static void submitTextSearch(Object dialog, String query) {
    }

}

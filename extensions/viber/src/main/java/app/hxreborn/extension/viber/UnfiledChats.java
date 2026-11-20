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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import android.view.View;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class UnfiledChats {

    private static final String FOLDER_ID = "hx_unfiled_chats";

    private static final int UNFILED_TAB_INDEX = 0;

    private static final int CUSTOM_FOLDER_TYPE = 100;

    private static final String UNFILED_MEMBERSHIP = "NOT EXISTS (SELECT 1 FROM folders_to_chats AS membership\n"
            + "INNER JOIN folders AS folder ON folder.id = membership.folder_id\n" + "WHERE folder.type = "
            + CUSTOM_FOLDER_TYPE + " AND membership.conversation_id = [conversations].[_id])\n";

    private static final Set<Object> unfiledQueries = Collections
        .synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static boolean selected;

    private UnfiledChats() {
    }

    static int addChatsButtonId() {
        return 0;
    }

    static int emptyTitleId() {
        return 0;
    }

    static int emptySubtitleId() {
        return 0;
    }

    public static boolean addTabAndIsEmpty(List<Object> tabs) {
        if (PatchSettings.unfiledChats()) {
            tabs.add(UNFILED_TAB_INDEX, createTab());
        }
        return tabs.isEmpty();
    }

    private static Object createTab() {
        return null;
    }

    public static String queryFolder(String folderId) {
        return FOLDER_ID.equals(folderId) ? null : folderId;
    }

    public static void rememberQuery(String folderId, Object query) {
        if (query != null && FOLDER_ID.equals(folderId)) {
            unfiledQueries.add(query);
        }
    }

    public static String filterSelection(Object query, String original) {
        if (original == null || !PatchSettings.unfiledChats() || !unfiledQueries.contains(query)) {
            return original;
        }
        int suffix = original.lastIndexOf(" GROUP BY ");
        if (suffix < 0) {
            suffix = original.lastIndexOf(" ORDER BY ");
        }
        if (suffix < 0) {
            suffix = original.length();
        }
        return UNFILED_MEMBERSHIP + " AND (" + original.substring(0, suffix) + ")" + original.substring(suffix);
    }

    public static void selectFolder(Object folder) {
        selected = folder != null && FOLDER_ID.equals(folderId(folder));
    }

    private static String folderId(Object folder) {
        return null;
    }

    public static void updateEmptyView(View root) {
        if (root == null) {
            return;
        }
        View addChats = root.findViewById(addChatsButtonId());
        if (addChats != null) {
            addChats.setVisibility(selected ? View.GONE : View.VISIBLE);
        }
        if (!selected) {
            return;
        }
        TextView title = root.findViewById(emptyTitleId());
        TextView subtitle = root.findViewById(emptySubtitleId());
        if (title != null) {
            title.setText("No unfiled chats");
        }
        if (subtitle != null) {
            subtitle.setText("Chats in your folders appear in All.");
        }
    }

}

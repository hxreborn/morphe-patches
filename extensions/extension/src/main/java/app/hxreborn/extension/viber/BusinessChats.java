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

import java.util.Set;
import java.util.TreeSet;

import android.util.Log;

@SuppressWarnings("unused")
public final class BusinessChats {

    private static final int BUSINESS_CHAT_FLAGS = (1 << 0) | (1 << 3) | (1 << 4);

    private static final long MARKED_UNREAD_FLAG = 1L << 33;

    private static final long VERIFICATION_CHAT_FLAG = 1L << 14;

    static final String CHAT_PREDICATE = "conversations.conversation_type = 0"
            + " AND (conversations.business_inbox_flags & " + BUSINESS_CHAT_FLAGS + ") <> 0";

    private BusinessChats() {
    }

    public static String filterSelection(String prefix) {
        return filterSelection(prefix, PatchSettings.filterBusinessChats(), PatchSettings.selectedBusinessChats());
    }

    static String filterSelection(String prefix, boolean enabled, Set<String> selected) {
        if (!enabled || prefix == null || selected.isEmpty()) {
            return prefix;
        }
        Set<Long> ids = new TreeSet<>();
        for (String selectedId : selected) {
            try {
                long id = Long.parseLong(selectedId);
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ex) {
                Log.w(PatchSettings.LOG_TAG, "Invalid selected business chat id");
            }
        }
        if (ids.isEmpty()) {
            return prefix;
        }
        StringBuilder joined = new StringBuilder();
        for (Long id : ids) {
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(id);
        }
        return prefix + "COALESCE((conversations._id IN (" + joined + ") AND " + CHAT_PREDICATE
                + " AND conversations.unread_events_count = 0 AND (conversations.flags & " + MARKED_UNREAD_FLAG
                + ") = 0 AND (conversations.flags2 & " + VERIFICATION_CHAT_FLAG + ") = 0), 0) = 0 AND ";
    }

    public static String preferenceKey(String key) {
        return PatchSettings.filterBusinessChats() ? PatchSettings.DISABLED_KEY_PREFIX + key : key;
    }

    public static boolean showPrompt(boolean original) {
        return original && !PatchSettings.filterBusinessChats();
    }

}

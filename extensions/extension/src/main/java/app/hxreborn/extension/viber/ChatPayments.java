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

@SuppressWarnings("unused")
public final class ChatPayments {

    private ChatPayments() {
    }

    public static boolean showSendMoney(boolean visible) {
        return visible && !PatchSettings.removeChatPayments();
    }

    public static List<?> filterAttachments(List<?> items, Object bankAttachment, Object payAttachment) {
        if (!PatchSettings.removeChatPayments() || items == null) {
            return items;
        }
        List<Object> filtered = new ArrayList<>(items.size());
        for (Object item : items) {
            if (item != bankAttachment && item != payAttachment) {
                filtered.add(item);
            }
        }
        return filtered;
    }

}

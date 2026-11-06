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
package app.hxreborn.extension.connected2;

@SuppressWarnings("unused")
public final class DeletedMessages {

    private static final String DELETED_MARK = " 🗑";

    private DeletedMessages() {

    }

    static String bodyOf(Object message) {
        return null;
    }

    static void saveBody(Object message, String body) {
    }

    public static void mark(Object message, Object contentType, Object textContentType) {
        if (message == null || contentType != textContentType) {
            return;
        }
        String body = bodyOf(message);
        if (body == null || body.endsWith(DELETED_MARK)) {
            return;
        }
        saveBody(message, body + DELETED_MARK);
    }

}

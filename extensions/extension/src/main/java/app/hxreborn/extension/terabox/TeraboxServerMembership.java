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

import java.lang.reflect.Field;

import android.util.Log;

@SuppressWarnings("unused")
public final class TeraboxServerMembership {

    private static final String TAG = "TeraboxServerMembership";

    private static final String NOT_A_MEMBER = "0";

    private TeraboxServerMembership() {
    }

    public static boolean isVip(Object vipInfo) {
        return vipInfo != null && readField(vipInfo, "isVip", Boolean.FALSE);
    }

    public static String vipType(Object vipInfo) {
        return (isVip(vipInfo)) ? String.valueOf(readField(vipInfo, "vipLevel", 0)) : NOT_A_MEMBER;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String name, T fallback) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            Log.w(TAG, "Could not read " + name, exception);
            return fallback;
        }
    }

}

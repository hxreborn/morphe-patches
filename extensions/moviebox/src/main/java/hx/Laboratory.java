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
package hx;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import android.content.SharedPreferences;
import android.util.Log;
import hx.util.Logs;

final class Laboratory {

    private static final String MMKV_CLASS = "com.tencent.mmkv.MMKV";

    private static final String MMKV_APP_ID = "kv_app";

    private static final String PASSWORD_TIME_KEY = "lab_enter_password_time";

    private Laboratory() {
    }

    static void unlock(ClassLoader loader) {
        try {
            Class<?> mmkvClass = Class.forName(MMKV_CLASS, false, loader);
            Method mmkvWithId = findFactory(mmkvClass);
            if (mmkvWithId == null) {
                throw new IllegalStateException("no mmkvWithID(String) on " + MMKV_CLASS);
            }
            SharedPreferences.Editor store = (SharedPreferences.Editor) mmkvWithId.invoke(null, MMKV_APP_ID);
            store.putLong(PASSWORD_TIME_KEY, Long.MAX_VALUE);
            Log.i(Logs.TAG, "Laboratory unlocked");
        } catch (Exception ex) {
            Log.e(Logs.TAG, "cannot unlock Laboratory", ex);
        }
    }

    private static Method findFactory(Class<?> mmkvClass) {
        for (Method candidate : mmkvClass.getDeclaredMethods()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if (Modifier.isStatic(candidate.getModifiers()) && candidate.getReturnType() == mmkvClass
                    && parameters.length == 1 && parameters[0] == String.class) {
                return candidate;
            }
        }
        return null;
    }

}

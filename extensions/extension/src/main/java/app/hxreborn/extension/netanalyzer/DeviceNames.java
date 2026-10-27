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
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

final class DeviceNames {

    private static final String TAG = "hxNetAnalyzer";

    private static final String PREFERENCES = "DeviceNameDatabase";

    private static final String DATABASE_KEY = "database";

    private static final String REGISTRATION_SEPARATOR = ";";

    private static Field liveMap;

    private DeviceNames() {
    }

    static void register(String registration) throws ReflectiveOperationException {
        final String[] parts = registration.split(REGISTRATION_SEPARATOR, 2);
        final Field field = Class.forName(parts[0], true, DeviceNames.class.getClassLoader())
            .getDeclaredField(parts[1]);
        field.setAccessible(true);
        liveMap = field;
    }

    static String get(String key) {
        final Map<String, String> map = map();
        return (map != null && key != null) ? map.get(key) : null;
    }

    static void put(Context context, String key, String name) {
        final Map<String, String> map = map();
        if (map == null || key == null) {
            return;
        }
        final String trimmed = (name != null) ? name : "";
        if (trimmed.isEmpty()) {
            map.remove(key);
        } else {
            map.put(key, trimmed);
        }
        final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        try {
            final JSONObject stored = new JSONObject(preferences.getString(DATABASE_KEY, "{}"));
            stored.put(DATABASE_KEY, new JSONObject(map));
            preferences.edit().putString(DATABASE_KEY, stored.toString()).apply();
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot save the device name database", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> map() {
        if (liveMap == null) {
            return null;
        }
        try {
            return (Map<String, String>) liveMap.get(null);
        } catch (IllegalAccessException ex) {
            Log.e(TAG, "Cannot read the device name database", ex);
            return null;
        }
    }

}

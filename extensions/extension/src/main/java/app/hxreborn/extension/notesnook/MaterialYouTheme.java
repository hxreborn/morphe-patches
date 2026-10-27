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
package app.hxreborn.extension.notesnook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class MaterialYouTheme {

    private static final String TAG = "MaterialYouTheme";

    private static final String SETTINGS_INSTANCE = "default";

    private static final String SETTINGS_KEY = "appSettings";

    private static final String STOCK_ACCENT = "#008837";

    private static final String LIGHT_THEME_KEY = "lighTheme";

    private static final String DARK_THEME_KEY = "darkTheme";

    private static final String LIGHT_THEME_ID = "default-light";

    private static final String DARK_THEME_ID = "default-dark";

    private MaterialYouTheme() {

    }

    public static void recolorDefaultThemes(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }

        try {
            MmkvSettings store = MmkvSettings.open();
            String stored = store.read();
            if (stored == null) {
                return;
            }

            JSONObject settings = new JSONObject(stored);
            boolean changed = recolor(settings, LIGHT_THEME_KEY, LIGHT_THEME_ID,
                    context.getColor(android.R.color.system_accent1_600));
            changed |= recolor(settings, DARK_THEME_KEY, DARK_THEME_ID,
                    context.getColor(android.R.color.system_accent1_500));
            if (changed) {
                store.write(settings.toString());
            }
        } catch (ReflectiveOperationException | JSONException | RuntimeException ex) {
            Log.w(TAG, "theme not applied", ex);
        }
    }

    static boolean recolor(JSONObject settings, String themeKey, String themeId, int accent) throws JSONException {
        JSONObject theme = settings.optJSONObject(themeKey);
        if (theme == null || !themeId.equals(theme.optString("id"))) {
            return false;
        }

        String current = accentOf(theme);
        if (current == null) {
            return false;
        }

        JSONObject scopes = theme.getJSONObject("scopes");
        String replacement = String.format(Locale.ROOT, "#%06x", accent & 0xFFFFFF);
        boolean changed = replaceColor(scopes, current, replacement);
        changed |= replaceColor(scopes, STOCK_ACCENT, replacement);
        return changed;
    }

    private static String accentOf(JSONObject theme) {
        try {
            return theme.getJSONObject("scopes").getJSONObject("base").getJSONObject("primary").getString("accent");
        } catch (JSONException ex) {
            return null;
        }
    }

    private static boolean replaceColor(JSONObject node, String from, String to) throws JSONException {
        if (from.equalsIgnoreCase(to)) {
            return false;
        }
        boolean changed = false;
        for (Iterator<String> keys = node.keys(); keys.hasNext();) {
            String name = keys.next();
            Object value = node.get(name);
            if (value instanceof JSONObject) {
                changed |= replaceColor((JSONObject) value, from, to);
            } else if (from.equalsIgnoreCase(value.toString())) {
                node.put(name, to);
                changed = true;
            }
        }
        return changed;
    }

    private static final class MmkvSettings {

        private static final String MMKV_CLASS = "com.ammarahmed.mmkv.MMKV";

        private static final int SINGLE_PROCESS_MODE = 1;

        private final Object mmkv;

        private final long handle;

        private final Method decodeString;

        private final Method encodeString;

        private MmkvSettings(Object mmkv, long handle, Method decodeString, Method encodeString) {
            this.mmkv = mmkv;
            this.handle = handle;
            this.decodeString = decodeString;
            this.encodeString = encodeString;
        }

        static MmkvSettings open() throws ReflectiveOperationException {
            Class<?> type = Class.forName(MMKV_CLASS);
            Method getHandle = accessibleMethod(type, "getMMKVWithID", String.class, int.class, String.class,
                    String.class);
            long handle = (Long) getHandle.invoke(null, SETTINGS_INSTANCE, SINGLE_PROCESS_MODE, null, null);

            Constructor<?> constructor = type.getDeclaredConstructor(long.class);
            constructor.setAccessible(true);
            return new MmkvSettings(constructor.newInstance(handle), handle,
                    accessibleMethod(type, "decodeString", long.class, String.class, String.class),
                    accessibleMethod(type, "encodeString", long.class, String.class, String.class));
        }

        String read() throws ReflectiveOperationException {
            return (String) this.decodeString.invoke(this.mmkv, this.handle, SETTINGS_KEY, null);
        }

        void write(String json) throws ReflectiveOperationException {
            this.encodeString.invoke(this.mmkv, this.handle, SETTINGS_KEY, json);
        }

        private static Method accessibleMethod(Class<?> type, String name, Class<?>... parameters)
                throws NoSuchMethodException {
            Method method = type.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        }

    }

}

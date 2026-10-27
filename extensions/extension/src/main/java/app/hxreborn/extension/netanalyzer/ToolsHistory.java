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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ToolsHistory {

    static final String ADDRESS = "address";

    static final String DATE = "date";

    static final String FAVORITE = "favorite";

    static final String TYPE_STACK = "type_stack";

    private static final String TAG = "hxNetAnalyzer";

    private static final String PREFERENCES = "ToolsHistory";

    private static final String HISTORY_KEY = "history";

    private static final String ROUTE_ARGUMENT = "?arg=";

    private static final String ROUTE_ITEM_KEY = "item";

    private static final String FIELD_SEPARATOR = ",";

    private static final String NAME_SEPARATOR = "=";

    private static final String REGISTRATION_SEPARATOR = ";";

    private static final int REGISTRATION_PARTS = 4;

    private static Class<?> historyClass;

    private static Class<?> routeClass;

    private static String navigateMethod;

    private static final Map<String, String> ITEM_FIELDS = new HashMap<>();

    private static final List<WeakReference<Object>> NAVIGATORS = new ArrayList<>();

    private ToolsHistory() {
    }

    static void register(String registration) throws ClassNotFoundException {
        final String[] parts = registration.split(REGISTRATION_SEPARATOR, REGISTRATION_PARTS);
        final ClassLoader loader = ToolsHistory.class.getClassLoader();
        historyClass = Class.forName(parts[0], false, loader);
        routeClass = Class.forName(parts[1], false, loader);
        navigateMethod = parts[2];
        ITEM_FIELDS.clear();
        for (String entry : parts[3].split(FIELD_SEPARATOR)) {
            final String[] names = entry.split(NAME_SEPARATOR, 2);
            ITEM_FIELDS.put(names[0], names[1]);
        }
    }

    static void forgetNavigators() {
        synchronized (NAVIGATORS) {
            NAVIGATORS.clear();
        }
    }

    static void captureNavigator(Object controller) {
        synchronized (NAVIGATORS) {
            for (int i = NAVIGATORS.size() - 1; i >= 0; i--) {
                if (NAVIGATORS.get(i).get() == null) {
                    NAVIGATORS.remove(i);
                }
            }
            NAVIGATORS.add(new WeakReference<>(controller));
        }
    }

    static List<JSONObject> load(Context context) {
        final List<JSONObject> entries = new ArrayList<>();
        try {
            final JSONArray history = new JSONObject(preferences(context).getString(HISTORY_KEY, "{}"))
                .optJSONArray(HISTORY_KEY);
            for (int i = 0; history != null && i < history.length(); i++) {
                entries.add(history.getJSONObject(i));
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot read the tools history", ex);
        }
        Collections.sort(entries, (left, right) -> right.optString(DATE).compareTo(left.optString(DATE)));
        return entries;
    }

    static void delete(Context context, JSONObject entry) {
        final List<JSONObject> entries = load(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (sameAddress(entries.get(i), entry)) {
                entries.remove(i);
            }
        }
        save(context, entries);
        try {
            final List<Object> items = liveItems();
            for (int i = items.size() - 1; i >= 0; i--) {
                if (address(items.get(i)).equalsIgnoreCase(entry.optString(ADDRESS))) {
                    items.remove(i);
                }
            }
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Cannot delete from the live tools history", ex);
        }
    }

    static void clearUnstarred(Context context) {
        for (JSONObject entry : load(context)) {
            if (!entry.optBoolean(FAVORITE)) {
                delete(context, entry);
            }
        }
    }

    static void setFavorite(Context context, JSONObject entry, boolean favorite) {
        final List<JSONObject> entries = load(context);
        for (JSONObject candidate : entries) {
            if (sameAddress(candidate, entry)) {
                try {
                    candidate.put(FAVORITE, favorite);
                } catch (JSONException ex) {
                    Log.e(TAG, "Cannot star a tools history entry", ex);
                }
            }
        }
        save(context, entries);
        try {
            final Field favoriteField = itemField(FAVORITE);
            for (Object item : liveItems()) {
                if (address(item).equalsIgnoreCase(entry.optString(ADDRESS))) {
                    favoriteField.setBoolean(item, favorite);
                }
            }
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Cannot update the star in the live tools history", ex);
        }
    }

    static boolean query(String address) {
        try {
            return open(new JSONObject().put(ADDRESS, address));
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot build the tools query for " + address, ex);
            return false;
        }
    }

    static boolean open(JSONObject entry) {
        if (routeClass == null) {
            return false;
        }
        final String route;
        try {
            route = routeClass.getName() + ROUTE_ARGUMENT
                    + Uri.encode(new JSONObject().put(ROUTE_ITEM_KEY, entry).toString());
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot build the tools query route", ex);
            return false;
        }
        final List<Object> controllers = new ArrayList<>();
        synchronized (NAVIGATORS) {
            for (WeakReference<Object> navigator : NAVIGATORS) {
                final Object controller = navigator.get();
                if (controller != null) {
                    controllers.add(controller);
                }
            }
        }
        for (Object controller : controllers) {
            try {
                navigateMethod(controller.getClass()).invoke(null, controller, route);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ex) {
                Log.d(TAG, "Navigator " + controller + " cannot open the tools query: " + ex.getCause());
            }
        }
        return false;
    }

    private static Method navigateMethod(Class<?> controllerClass) throws NoSuchMethodException {
        for (Class<?> type = controllerClass; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                final Class<?>[] parameters = method.getParameterTypes();
                if (method.getName().equals(navigateMethod) && Modifier.isStatic(method.getModifiers())
                        && parameters.length == 2 && parameters[0].isAssignableFrom(controllerClass)
                        && parameters[1] == String.class) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(navigateMethod + " on " + controllerClass);
    }

    private static void save(Context context, List<JSONObject> entries) {
        try {
            final JSONArray history = new JSONArray();
            for (JSONObject entry : entries) {
                history.put(entry);
            }
            preferences(context).edit()
                .putString(HISTORY_KEY, new JSONObject().put(HISTORY_KEY, history).toString())
                .apply();
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot save the tools history", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> liveItems() throws ReflectiveOperationException {
        for (Field field : historyClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && List.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return (List<Object>) field.get(null);
            }
        }
        throw new NoSuchFieldException("No history list in " + historyClass);
    }

    private static String address(Object item) throws ReflectiveOperationException {
        final Object value = itemField(ADDRESS).get(item);
        return (value != null) ? value.toString() : "";
    }

    private static Field itemField(String property) throws NoSuchFieldException {
        final String name = ITEM_FIELDS.get(property);
        if (name == null) {
            throw new NoSuchFieldException(property);
        }
        final Field field = itemClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Class<?> itemClass() throws NoSuchFieldException {
        try {
            final List<Object> items = liveItems();
            if (!items.isEmpty()) {
                return items.get(0).getClass();
            }
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Cannot read the live tools history", ex);
        }
        throw new NoSuchFieldException("The live tools history is empty");
    }

    private static boolean sameAddress(JSONObject left, JSONObject right) {
        return left.optString(ADDRESS).equalsIgnoreCase(right.optString(ADDRESS));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

}

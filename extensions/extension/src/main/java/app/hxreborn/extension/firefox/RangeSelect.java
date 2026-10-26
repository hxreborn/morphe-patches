/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/RangeSelect.java
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.SystemClock;
import android.util.Log;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public final class RangeSelect {

    private static final String TAG = "FirefoxPatches";

    private static final long ARMED = Long.MAX_VALUE;

    private static final long GRACE_MS = 300L;

    private static final String ADD_SELECT = "org.mozilla.fenix.tabstray.redux.action.TabsTrayAction$AddSelectTab";

    private static final String LIST_STATE = "androidx.compose.foundation.lazy.LazyListState";

    private static final RangeSelectNoOp NOOP = new RangeSelectNoOp();

    private static volatile long suppressUntil;

    private static volatile Constructor<?> addSelect;

    private RangeSelect() {
    }

    public static boolean longPress(Object owner, Object item) {
        if (!PatchSettings.rangeSelect()) {
            return false;
        }
        boolean selecting = false;
        try {
            Object store = store(owner);
            Object state = Reflection.call(store, "getState");
            Set selected = (Set) Reflection.call(Reflection.read(state, "mode"), "getSelectedTabs");
            if (selected == null || selected.isEmpty()) {
                return false;
            }
            selecting = true;
            Object page = Reflection.read(state, "selectedPage");
            if (!(page instanceof Enum) || !"NormalTabs".equals(((Enum) page).name())) {
                return true;
            }
            select(store, state, selected, item);
            return true;
        } catch (Throwable ex) {
            Log.e(TAG, "range select failed", ex);
            return selecting;
        }
    }

    public static boolean consume() {
        long until = suppressUntil;
        if (until > 0L && SystemClock.uptimeMillis() < until) {
            suppressUntil = 0L;
            return true;
        }
        return false;
    }

    static void endGesture() {
        if (suppressUntil == ARMED) {
            suppressUntil = SystemClock.uptimeMillis() + GRACE_MS;
        }
    }

    public static Object noop() {
        return NOOP;
    }

    public static Object start(Object gesture) {
        Object listState = null;
        Object state = null;
        try {
            for (Field field : gesture.getClass().getDeclaredFields()) {
                if (field.getType().isPrimitive()) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(gesture);
                if (LIST_STATE.equals(field.getType().getName())) {
                    listState = value;
                } else {
                    state = value;
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, "range select start failed", ex);
        }
        return new RangeSelectStart(listState, state);
    }

    private static void select(Object store, Object state, Set selected, Object item) throws Exception {
        HashSet<String> chosen = new HashSet<>();
        Object last = null;
        for (Object tab : selected) {
            last = tab;
            chosen.add((String) Reflection.call(tab, "getId"));
        }
        String anchorId = (String) Reflection.call(last, "getId");
        String itemId = (String) Reflection.call(item, "getId");
        List items = (List) Reflection.read(Reflection.read(state, "normalTabsState"), "items");
        int a = -1;
        int b = -1;
        for (int i = 0, n = items.size(); i < n; i++) {
            String id = (String) Reflection.call(items.get(i), "getId");
            if (a < 0 && anchorId.equals(id)) {
                a = i;
            }
            if (b < 0 && itemId.equals(id)) {
                b = i;
            }
        }
        if (a < 0 || b < 0) {
            return;
        }

        Constructor<?> constructor = addSelect(store);
        Class<?> tabType = constructor.getParameterTypes()[0];
        int step = (a <= b) ? 1 : -1;
        for (int i = a;; i += step) {
            Object current = items.get(i);
            if (tabType.isInstance(current) && !chosen.contains((String) Reflection.call(current, "getId"))) {
                dispatch(store, constructor.newInstance(current));
            }
            if (i == b) {
                break;
            }
        }
        suppressUntil = ARMED;
    }

    private static Constructor<?> addSelect(Object store) throws Exception {
        Constructor<?> cached = addSelect;
        if (cached != null) {
            return cached;
        }
        Class<?> type = Class.forName(ADD_SELECT, true, store.getClass().getClassLoader());
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 1) {
                constructor.setAccessible(true);
                addSelect = constructor;
                return constructor;
            }
        }
        throw new NoSuchMethodException("AddSelectTab constructor");
    }

    private static void dispatch(Object store, Object action) throws Exception {
        for (Class<?> type = store.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if ("dispatch".equals(method.getName()) && p.length == 1 && p[0].isInstance(action)) {
                    method.setAccessible(true);
                    method.invoke(store, action);
                    return;
                }
            }
        }
        throw new NoSuchMethodException("Store.dispatch");
    }

    private static Object store(Object owner) throws Exception {
        Method own = Reflection.find(owner.getClass(), "getTabsTrayStore$app");
        if (own != null) {
            return own.invoke(owner);
        }
        for (Field field : owner.getClass().getDeclaredFields()) {
            if (field.getType().isPrimitive()) {
                continue;
            }
            field.setAccessible(true);
            Object holder = field.get(owner);
            if (holder == null) {
                continue;
            }
            Method method = Reflection.find(holder.getClass(), "getTabsTrayStore$app");
            if (method != null) {
                return method.invoke(holder);
            }
        }
        throw new NoSuchFieldException("TabManagementFragment");
    }

}

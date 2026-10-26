/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/ScrollMemory.java
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@SuppressWarnings("unused")
public final class ScrollMemory {

    private static final Map<Long, Object> STATES = new HashMap<>();

    private static final Set<Object> REUSED = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());

    private static Method compositeKeyMethod;

    private static Access listAccess;

    private static Access gridAccess;

    private ScrollMemory() {
    }

    public static synchronized Object pick(Object composer, Object fresh) {
        if (!PatchSettings.scrollMemory()) {
            STATES.clear();
            return fresh;
        }
        try {
            Long key = compositeKey(composer);
            Object stored = STATES.get(key);
            if (stored == fresh) {
                return fresh;
            }
            if (stored != null && stored.getClass() == fresh.getClass()) {
                REUSED.add(stored);
                return stored;
            }
            STATES.put(key, fresh);
            return fresh;
        } catch (Throwable ex) {
            return fresh;
        }
    }

    public static synchronized int resolveScrollIndex(int index, boolean offsetA, boolean offsetB, Object state) {
        if (!PatchSettings.scrollMemory() || !REUSED.contains(state)) {
            return index;
        }
        if (index < 0) {
            return -1;
        }
        int adjusted = index + (offsetA ? 1 : 0) + (offsetB ? 1 : 0);
        try {
            return isVisible(state, adjusted) ? -1 : index;
        } catch (Throwable ex) {
            return -1;
        }
    }

    private static Long compositeKey(Object composer) throws Exception {
        Method method = compositeKeyMethod;
        if (method == null) {
            ClassLoader loader = composer.getClass().getClassLoader();
            method = Class.forName("androidx.compose.runtime.Composer", false, loader)
                .getMethod("getCompositeKeyHashCode");
            compositeKeyMethod = method;
        }
        return ((Number) method.invoke(composer)).longValue();
    }

    private static boolean isVisible(Object state, int target) throws Exception {
        ClassLoader loader = state.getClass().getClassLoader();
        if (listAccess == null) {
            listAccess = new Access(loader, "androidx.compose.foundation.lazy.LazyListState",
                    "androidx.compose.foundation.lazy.LazyListLayoutInfo",
                    "androidx.compose.foundation.lazy.LazyListItemInfo");
        }
        if (listAccess.stateClass.isInstance(state)) {
            return listAccess.contains(state, target);
        }

        if (gridAccess == null) {
            gridAccess = new Access(loader, "androidx.compose.foundation.lazy.grid.LazyGridState",
                    "androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo",
                    "androidx.compose.foundation.lazy.grid.LazyGridItemInfo");
        }
        if (gridAccess.stateClass.isInstance(state)) {
            return gridAccess.contains(state, target);
        }

        return true;
    }

    private static final class Access {

        final Class<?> stateClass;

        final Method layoutInfo;

        final Method visibleItems;

        final Method itemIndex;

        Access(ClassLoader loader, String state, String layout, String item) throws Exception {
            this.stateClass = Class.forName(state, false, loader);
            this.layoutInfo = this.stateClass.getMethod("getLayoutInfo");
            this.visibleItems = Class.forName(layout, false, loader).getMethod("getVisibleItemsInfo");
            this.itemIndex = Class.forName(item, false, loader).getMethod("getIndex");
        }

        boolean contains(Object state, int target) throws Exception {
            Object info = this.layoutInfo.invoke(state);
            List<?> items = (List<?>) this.visibleItems.invoke(info);
            for (int i = 0, n = items.size(); i < n; i++) {
                if (((Number) this.itemIndex.invoke(items.get(i))).intValue() == target) {
                    return true;
                }
            }
            return false;
        }

    }

}

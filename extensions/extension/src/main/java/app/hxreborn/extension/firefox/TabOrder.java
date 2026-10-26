/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/TabOrder.java
 */
package app.hxreborn.extension.firefox;

import java.util.ArrayList;
import java.util.Collection;

import android.util.Log;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public final class TabOrder {

    private static final String TAG = "FirefoxPatches";

    private static volatile boolean endPending;

    private TabOrder() {
    }

    public static void markEnd() {
        endPending = true;
    }

    public static void clearEnd() {
        endPending = false;
    }

    public static ArrayList add(Object tab, Collection tabs, String selectedId) {
        ArrayList list = new ArrayList(tabs);
        int at = list.size();
        try {
            boolean atEnd = consumeEnd();
            if (!atEnd && selectedId != null && PatchSettings.tabNext()) {
                int selected = indexOfSelected(list, tab, selectedId);
                if (selected >= 0) {
                    at = selected + 1;
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, "tab order failed", ex);
            at = list.size();
        }
        list.add(at, tab);
        return list;
    }

    private static boolean consumeEnd() {
        boolean marked = endPending;
        endPending = false;
        return marked;
    }

    private static int indexOfSelected(ArrayList list, Object tab, String selectedId) throws Exception {
        boolean isPrivate = isPrivate(tab);
        for (int i = 0, n = list.size(); i < n; i++) {
            Object item = list.get(i);
            if (selectedId.equals(Reflection.read(item, "id"))) {
                return (isPrivate(item) == isPrivate) ? i : -1;
            }
        }
        return -1;
    }

    static boolean isPrivate(Object tab) throws Exception {
        Object content = Reflection.read(tab, "content");
        return Boolean.TRUE.equals(Reflection.read(content, "private"));
    }

}

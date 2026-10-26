/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.util.Log;

final class Drawables {

    private static final Map<String, Integer> IDS = new ConcurrentHashMap<>();

    private Drawables() {

    }

    static int id(String name) {
        try {
            return id(Reflection.application(), name);
        } catch (ReflectiveOperationException ex) {
            Log.e("FirefoxPatches", "drawable " + name + " failed", ex);
            return 0;
        }
    }

    static int id(Context context, String name) {
        Integer cached = IDS.get(name);
        if (cached != null) {
            return cached;
        }
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id != 0) {
            IDS.put(name, id);
        }
        return id;
    }

}

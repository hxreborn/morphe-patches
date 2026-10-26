/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.util.Log;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public final class TabHistory {

    private static final String TAG = "FirefoxPatches";

    private static final int LIMIT = 64;

    private static final LinkedHashSet<String> RECENT = new LinkedHashSet<>();

    private static volatile String latest;

    private TabHistory() {
    }

    public static void observe(String selectedId) {
        if (selectedId == null || selectedId.equals(latest)) {
            return;
        }
        synchronized (RECENT) {
            RECENT.remove(selectedId);
            RECENT.add(selectedId);
            if (RECENT.size() > LIMIT) {
                Iterator<String> eldest = RECENT.iterator();
                eldest.next();
                eldest.remove();
            }
            latest = selectedId;
        }
    }

    public static boolean selectParent(boolean stock) {
        return stock && !PatchSettings.selectPreviousTab();
    }

    public static String previous(ArrayList tabs, boolean isPrivate, Set excluded) {
        if (!PatchSettings.selectPreviousTab()) {
            return null;
        }
        try {
            return mostRecent(tabs, isPrivate, excluded);
        } catch (Throwable ex) {
            Log.e(TAG, "previous tab failed", ex);
            return null;
        }
    }

    private static String mostRecent(List tabs, boolean isPrivate, Set excluded) throws Exception {
        Set<String> open = new LinkedHashSet<>();
        Set<String> usable = new LinkedHashSet<>();
        for (Object tab : tabs) {
            String id = (String) Reflection.read(tab, "id");
            open.add(id);
            if (TabOrder.isPrivate(tab) == isPrivate && !excluded.contains(id)) {
                usable.add(id);
            }
        }
        synchronized (RECENT) {
            RECENT.retainAll(open);
            List<String> recent = new ArrayList<>(RECENT);
            for (int i = recent.size() - 1; i >= 0; i--) {
                if (usable.contains(recent.get(i))) {
                    return recent.get(i);
                }
            }
        }
        return null;
    }

}

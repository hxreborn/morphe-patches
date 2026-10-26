/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/TabScroll.java
 */
package app.hxreborn.extension.firefox;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public final class TabScroll {

    private static final String TAG = "FirefoxPatches";

    private static final int HISTORY = 6;

    private static final float FAR = 1.0e7f;

    private static final List<TrackedState> STATES = new ArrayList<>();

    private static volatile Object tray;

    private TabScroll() {
    }

    public static synchronized void track(Object state) {
        if (state == null) {
            return;
        }
        Object page = selectedPage();
        Iterator<TrackedState> it = STATES.iterator();
        while (it.hasNext()) {
            TrackedState known = it.next();
            Object knownState = known.state.get();
            if (knownState == null) {
                it.remove();
            } else if (knownState == state) {
                page = known.page;
                it.remove();
            }
        }
        STATES.add(new TrackedState(state, page));
        while (STATES.size() > HISTORY) {
            STATES.remove(0);
        }
    }

    public static void tray(Object state) {
        tray = state;
    }

    public static TabScrollClick up() {
        return TabScrollClick.UP;
    }

    public static TabScrollClick down() {
        return TabScrollClick.DOWN;
    }

    public static int upIcon() {
        return Drawables.id("hx_ic_scroll_up");
    }

    public static int downIcon() {
        return Drawables.id("hx_ic_scroll_down");
    }

    static void go(boolean down) {
        try {
            Object state = current();
            if (state == null) {
                return;
            }
            scrollTo(state, down ? Math.max(0, total(state) - 1) : 0);
        } catch (Throwable ex) {
            Log.e(TAG, "tab scroll failed", ex);
        }
    }

    private static synchronized Object current() {
        Object page = selectedPage();
        Object newest = null;
        for (int i = STATES.size() - 1; i >= 0; i--) {
            TrackedState tracked = STATES.get(i);
            Object state = tracked.state.get();
            if (state == null || !hasItems(state)) {
                continue;
            }
            if (page == tracked.page) {
                return state;
            }
            if (newest == null) {
                newest = state;
            }
        }
        return newest;
    }

    private static Object selectedPage() {
        try {
            Object state = tray;
            return (state != null) ? Reflection.read(state, "selectedPage") : null;
        } catch (Throwable ex) {
            return null;
        }
    }

    private static boolean hasItems(Object state) {
        try {
            return total(state) > 0;
        } catch (Throwable ex) {
            return false;
        }
    }

    private static int total(Object state) throws Exception {
        Object info = Reflection.call(state, "getLayoutInfo");
        return ((Number) Reflection.call(info, "getTotalItemsCount")).intValue();
    }

    private static void scrollTo(Object state, int index) throws Exception {
        try {
            Method request = state.getClass().getMethod("requestScrollToItem", int.class, int.class);
            request.setAccessible(true);
            request.invoke(state, index, 0);
            return;
        } catch (NoSuchMethodException ignored) {
        }
        Method raw = Class
            .forName("androidx.compose.foundation.gestures.ScrollableState", false, state.getClass().getClassLoader())
            .getMethod("dispatchRawDelta", float.class);
        raw.invoke(state, (index <= 0) ? -FAR : FAR);
    }

    private static final class TrackedState {

        private final WeakReference<Object> state;

        private final Object page;

        TrackedState(Object state, Object page) {
            this.state = new WeakReference<>(state);
            this.page = page;
        }

    }

}

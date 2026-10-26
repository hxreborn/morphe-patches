/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/TabCloseDuplicates.java
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public final class TabCloseDuplicates implements Function0<Object> {

    private static final String TAG = "FirefoxPatches";

    private final boolean privateMode;

    private TabCloseDuplicates(boolean privateMode) {
        this.privateMode = privateMode;
    }

    public static TabCloseDuplicates of(boolean privateMode) {
        return new TabCloseDuplicates(privateMode);
    }

    public Object invoke() {
        try {
            Application app = Reflection.application();
            Object store = store(app);
            Object state = Reflection.call(store, "getState");
            List<Object> tabs = (List<Object>) Reflection.read(state, "tabs");
            String selectedId = (String) Reflection.read(state, "selectedTabId");

            Set<String> seen = new HashSet<>();
            List<String> duplicates = new ArrayList<>();
            if (selectedId != null) {
                for (Object tab : tabs) {
                    if (selectedId.equals(Reflection.read(tab, "id"))) {
                        String key = key(tab);
                        if (key != null) {
                            seen.add(key);
                        }
                        break;
                    }
                }
            }
            for (Object tab : tabs) {
                String id = (String) Reflection.read(tab, "id");
                if (selectedId != null && selectedId.equals(id)) {
                    continue;
                }
                String key = key(tab);
                if (key != null && !seen.add(key)) {
                    duplicates.add(id);
                }
            }

            if (!duplicates.isEmpty()) {
                dispatchRemove(store, duplicates);
            }
            toast(app, duplicates.size());
        } catch (Throwable ex) {
            Log.e(TAG, "close duplicates failed", ex);
        }
        return Unit.INSTANCE;
    }

    private String key(Object tab) throws Exception {
        Object content = Reflection.read(tab, "content");
        boolean isPrivate = Boolean.TRUE.equals(Reflection.read(content, "private"));
        if (isPrivate != this.privateMode) {
            return null;
        }
        return (String) Reflection.read(content, "url");
    }

    private static void dispatchRemove(Object store, List<String> ids) throws Exception {
        ClassLoader loader = store.getClass().getClassLoader();
        Class<?> removeType = Class.forName("mozilla.components.browser.state.action.TabListAction$RemoveTabsAction",
                true, loader);
        Object action = removeType.getConstructor(List.class, Set.class).newInstance(ids, Collections.emptySet());
        for (Method method : store.getClass().getMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if ("dispatch".equals(method.getName()) && p.length == 1 && p[0].isInstance(action)) {
                method.invoke(store, action);
                return;
            }
        }
        throw new NoSuchMethodException("Store.dispatch");
    }

    private static void toast(Application app, int count) {
        String text = (count == 0) ? "No duplicate tabs"
                : "Closed " + count + ((count == 1) ? " duplicate tab" : " duplicate tabs");
        Toast.makeText(app, text, Toast.LENGTH_SHORT).show();
    }

    private static Object store(Application app) throws Exception {
        Object components = Reflection.call(app, "getComponents");
        Object core = Reflection.call(components, "getCore");
        return Reflection.call(core, "getStore");
    }

}

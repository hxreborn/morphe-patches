/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/TabMenu.java
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
public final class TabMenu {

    private static final String TAG = "FirefoxPatches";

    private static final String CLOSE_ALL_TAG = "tabstray.banner.threeDotButton.closeAllTabs";

    private static final String DUPLICATE_TAG = "tabstray.banner.threeDotButton.closeDuplicateTabs";

    private static final String DUPLICATE_LABEL = "Close duplicate tabs";

    private static volatile Object trayState;

    private TabMenu() {
    }

    public static void remember(Object state) {
        trayState = state;
    }

    public static List withDuplicates(Object menu) {
        List items = (List) menu;
        if (!PatchSettings.closeDuplicates()) {
            return items;
        }
        try {
            int at = indexOfCloseAll(items);
            if (at < 0) {
                return items;
            }
            ArrayList result = new ArrayList(items);
            result.add(at, newItem(items.get(at), isPrivatePage()));
            return result;
        } catch (Throwable ex) {
            Log.e(TAG, "close duplicates item failed", ex);
            return items;
        }
    }

    private static boolean isPrivatePage() throws Exception {
        Object state = trayState;
        if (state == null) {
            return false;
        }
        Object page = Reflection.read(state, "selectedPage");
        return page instanceof Enum && "PrivateTabs".equals(((Enum) page).name());
    }

    private static int indexOfCloseAll(List items) throws Exception {
        for (int i = 0, n = items.size(); i < n; i++) {
            if (hasString(items.get(i), CLOSE_ALL_TAG)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasString(Object item, String value) throws Exception {
        if (item == null) {
            return false;
        }
        Class<?> type = item.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != String.class) {
                    continue;
                }
                field.setAccessible(true);
                if (value.equals(field.get(item))) {
                    return true;
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static Object newItem(Object like, boolean isPrivate) throws Exception {
        ClassLoader loader = like.getClass().getClassLoader();
        Class<?> textType = Class.forName("mozilla.components.compose.base.text.Text$String", true, loader);
        Object text = textType.getConstructor(String.class).newInstance(DUPLICATE_LABEL);
        int icon = Class.forName("mozilla.components.ui.icons.R$drawable", true, loader)
            .getField("mozac_ic_delete_24")
            .getInt(null);
        Object action = TabCloseDuplicates.of(isPrivate);
        for (Constructor<?> constructor : like.getClass().getDeclaredConstructors()) {
            Class<?>[] p = constructor.getParameterTypes();
            if (p.length == 6 && p[1] == int.class && p[2] == String.class && p[3] == textType && p[5] == int.class) {
                constructor.setAccessible(true);
                return constructor.newInstance(text, icon, DUPLICATE_TAG, null, action, 0x34);
            }
        }
        throw new NoSuchMethodException("IconItem constructor");
    }

}

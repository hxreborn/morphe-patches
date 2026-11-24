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
package app.hxreborn.extension.outlook.settings;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import app.hxreborn.extension.shared.ProxyHandler;

@SuppressWarnings("unused")
public final class PatchesMenu {

    static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static final String SETTINGS_ROW_PATH = "settings/hxreborn_patches";

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private PatchesMenu() {
    }

    public static Object settingsRowTitle(Class<?> titleType) {
        return proxy(titleType, new RowTitleHandler());
    }

    public static Object settingsRowIcon(Class<?> iconType) {
        return proxy(iconType, new RowIconHandler());
    }

    public static List<Object> addSettingsRow(List<Object> rows) {
        Object row = settingsRow();
        Object category = category(row);
        int lastIndexInCategory = -1;
        for (int index = 0; index < rows.size(); index++) {
            if (category(rows.get(index)) == category) {
                lastIndexInCategory = index;
            }
        }
        if (lastIndexInCategory < 0) {
            Log.w(PatchSettings.LOG_TAG, "Settings list has no section for the patches row");
            return rows;
        }
        List<Object> withPatchesRow = new ArrayList<>(rows);
        withPatchesRow.add(lastIndexInCategory + 1, row);
        return withPatchesRow;
    }

    public static Object clickedViewModel(Object viewModel, Object component) {
        if (!SETTINGS_ROW_PATH.equals(deepLinkUri(component))) {
            return viewModel;
        }
        MAIN_HANDLER.post(PatchesMenu::show);
        return null;
    }

    private static Object settingsRow() {
        return null;
    }

    private static Object category(Object component) {
        return null;
    }

    private static String deepLinkUri(Object component) {
        return null;
    }

    private static void drawSettingsRowIcon(Object composer) {
    }

    private static Object unit() {
        return null;
    }

    private static void show() {
        Activity activity = PatchContext.resumedActivity();
        if (activity == null || activity.isFinishing()) {
            Log.w(PatchSettings.LOG_TAG, "No resumed activity to show the patches menu in");
            return;
        }
        new PatchesSettingsDialog(activity).show();
    }

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(PatchesMenu.class.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static final class RowTitleHandler extends ProxyHandler {

        @Override
        protected String describe() {
            return SETTINGS_ROW_TITLE;
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            return SETTINGS_ROW_TITLE;
        }

    }

    private static final class RowIconHandler extends ProxyHandler {

        @Override
        protected String describe() {
            return SETTINGS_ROW_TITLE;
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            drawSettingsRowIcon(args[0]);
            return unit();
        }

    }

}

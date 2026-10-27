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
package app.hxreborn.extension.proton;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.shared.ProxyHandler;

@SuppressWarnings("unused")
public final class PatchesMenu {

    public static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    static final String ACTION_SHOW_PATCHES_SETTINGS = "app.hxreborn.extension.proton.SHOW_PATCHES_SETTINGS";

    private static final String TAG = "PatchesMenu";

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static volatile Object settingsRowOnClickProxy;

    private PatchesMenu() {

    }

    public static String bundleVersion() {
        return "unknown";
    }

    public static String hostActivity() {
        return "";
    }

    public static synchronized Object settingsRowOnClick(Class<?> onClickType) {
        if (settingsRowOnClickProxy == null) {
            settingsRowOnClickProxy = Proxy.newProxyInstance(PatchesMenu.class.getClassLoader(),
                    new Class<?>[] { onClickType }, new RowClickHandler());
        }
        return settingsRowOnClickProxy;
    }

    private static void show() {
        final Activity activity = PatchContext.resumedActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.startActivity(settingsIntent(activity));
            return;
        }

        final Context context = Utils.getContext();
        if (context == null) {
            Log.w(TAG, "No context to open the patches menu with");
            return;
        }

        final Intent intent = settingsIntent(context);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static Intent settingsIntent(Context context) {
        final Intent intent = new Intent(context, PatchesSettingsActivity.class);
        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            intent.setClassName(context, hostActivity()).setAction(ACTION_SHOW_PATCHES_SETTINGS);
        }
        return intent;
    }

    private static final class RowClickHandler extends ProxyHandler {

        @Override
        protected String describe() {
            return SETTINGS_ROW_TITLE;
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            MAIN_HANDLER.post(PatchesMenu::show);
            return null;
        }

    }

}

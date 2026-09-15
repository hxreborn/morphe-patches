/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import app.morphe.extension.shared.Utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@SuppressWarnings("unused")
public final class PatchesMenu {

    private static final String TAG = "PatchesMenu";

    public static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile Object settingsRowAction;

    private PatchesMenu() {}

    public static String bundleVersion() {
        return "unknown";
    }

    public static synchronized Object settingsRowAction(Class<?> actionType) {
        if (settingsRowAction == null) {
            settingsRowAction = Proxy.newProxyInstance(PatchesMenu.class.getClassLoader(),
                    new Class<?>[] { actionType }, new RowClickHandler());
        }
        return settingsRowAction;
    }

    private static final class RowClickHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            if ("toString".equals(name)) return SETTINGS_ROW_TITLE;
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return proxy == args[0];

            MAIN.post(PatchesMenu::show);
            return null;
        }
    }

    private static void show() {
        final Activity activity = PatchApplication.resumedActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.startActivity(new Intent(activity, PatchesSettingsActivity.class));
            return;
        }

        final Context context = Utils.getContext();
        if (context == null) {
            Log.w(TAG, "No context to open the patches menu with");
            return;
        }

        final Intent intent = new Intent(context, PatchesSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

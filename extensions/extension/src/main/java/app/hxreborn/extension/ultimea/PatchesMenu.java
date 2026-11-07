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
package app.hxreborn.extension.ultimea;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;

@SuppressWarnings("unused")
public final class PatchesMenu {

    public static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static WeakReference<Activity> resumedActivity = new WeakReference<>(null);

    private PatchesMenu() {
    }

    static void attach(Application application) {
        application.registerActivityLifecycleCallbacks(new ActivityCallbacksAdapter() {
            @Override
            public void onActivityResumed(Activity activity) {
                resumedActivity = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (resumedActivity.get() == activity) {
                    resumedActivity = new WeakReference<>(null);
                }
            }
        });
    }

    public static boolean open(Context context, String method) {
        if (!"/contact".equals(method)) {
            return false;
        }
        Activity activity = resumedActivity.get();
        Context launchContext = (activity != null) ? activity : context;
        Intent intent = new Intent(launchContext, PatchesSettingsActivity.class);
        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        launchContext.startActivity(intent);
        return true;
    }

    public static Object suppressedLoganResult(String method) {
        if ("getUploadPath".equals(method)) {
            return "";
        }
        if ("flush".equals(method) || "cleanAllLogs".equals(method)) {
            return null;
        }
        return Boolean.TRUE;
    }

}

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
package app.hxreborn.extension.duolingo;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;

@SuppressWarnings("unused")
public final class PatchContext {

    private static WeakReference<Activity> resumedActivity = new WeakReference<>(null);

    private PatchContext() {
    }

    public static void attach(Application application) {
        Utils.setContext(application);
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

    static Activity resumedActivity() {
        return resumedActivity.get();
    }

}

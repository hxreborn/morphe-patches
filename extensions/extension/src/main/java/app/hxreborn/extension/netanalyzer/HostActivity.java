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
package app.hxreborn.extension.netanalyzer;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Intent;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;

final class HostActivity {

    private static WeakReference<Activity> hostActivity = new WeakReference<>(null);

    private static boolean lifecycleRegistered;

    private static volatile boolean hostRefreshPending;

    private HostActivity() {
    }

    static void attach(Activity activity) {
        hostActivity = new WeakReference<>(activity);
        ToolsHistory.forgetNavigators();
        LanNames.load(activity);
        if (!lifecycleRegistered) {
            lifecycleRegistered = true;
            activity.getApplication().registerActivityLifecycleCallbacks(new HostRefresh(activity.getClass()));
        }
    }

    static void refreshHostOnResume() {
        hostRefreshPending = true;
    }

    static Activity current() {
        return hostActivity.get();
    }

    static void open(String activityClass, Intent extras) {
        final Activity activity = hostActivity.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        final Intent intent = (extras != null) ? new Intent(extras) : new Intent();
        activity.startActivity(intent.setClassName(activity, activityClass));
    }

    private static final class HostRefresh extends ActivityCallbacksAdapter {

        private final Class<?> hostClass;

        HostRefresh(Class<?> hostClass) {
            this.hostClass = hostClass;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (hostRefreshPending && activity.getClass() == this.hostClass) {
                hostRefreshPending = false;
                activity.recreate();
            }
        }

    }

}

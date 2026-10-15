/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

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

    private static final class HostRefresh implements Application.ActivityLifecycleCallbacks {

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

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

    }

}

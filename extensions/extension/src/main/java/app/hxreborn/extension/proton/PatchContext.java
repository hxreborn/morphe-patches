/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import app.morphe.extension.shared.Utils;

import java.lang.ref.WeakReference;

@SuppressWarnings("unused")
public final class PatchContext {

    private static WeakReference<Activity> resumedActivityReference = new WeakReference<>(null);

    private PatchContext() {}

    static Activity resumedActivity() {
        return resumedActivityReference.get();
    }

    public static void attach(Application application) {
        Utils.setContext(application);
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                AmoledBackgroundOverlay.apply(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                resumedActivityReference = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                clearResumedActivity(activity);
            }

            @Override
            public void onActivityStopped(Activity activity) {
                clearResumedActivity(activity);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                clearResumedActivity(activity);
            }
        });
    }

    private static void clearResumedActivity(Activity activity) {
        if (resumedActivityReference.get() == activity) {
            resumedActivityReference = new WeakReference<>(null);
        }
    }
}

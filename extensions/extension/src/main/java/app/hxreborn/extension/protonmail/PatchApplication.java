/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import app.hxreborn.extension.shared.SpoofSignature;
import app.morphe.extension.shared.Utils;

import java.lang.ref.WeakReference;

@SuppressWarnings("unused")
public class PatchApplication extends SpoofSignature {

    private static WeakReference<Activity> resumed = new WeakReference<>(null);

    public static Activity resumedActivity() {
        return resumed.get();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Utils.setContext(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                resumed = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (resumed.get() == activity) {
                    resumed = new WeakReference<>(null);
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (resumed.get() == activity) {
                    resumed = new WeakReference<>(null);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (resumed.get() == activity) {
                    resumed = new WeakReference<>(null);
                }
            }
        });
    }
}

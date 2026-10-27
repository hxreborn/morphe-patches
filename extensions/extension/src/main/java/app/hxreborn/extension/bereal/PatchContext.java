/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;

@SuppressWarnings("unused")
public final class PatchContext {

    private static WeakReference<Activity> resumedActivityReference = new WeakReference<>(null);

    private PatchContext() {
    }

    public static void attach(Application application) {
        Utils.setContext(application);
        application.registerActivityLifecycleCallbacks(new ActivityCallbacksAdapter() {
            @Override
            public void onActivityResumed(Activity activity) {
                resumedActivityReference = new WeakReference<>(activity);
            }
        });
    }

    public static void openSettings() {
        open(PatchesSettingsActivity.class);
    }

    static Activity activityFor(Object hostContext) {
        Object context = hostContext;
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return resumedActivityReference.get();
    }

    static void open(Class<? extends Activity> screen) {
        final Activity activity = resumedActivityReference.get();
        final Context context = (activity != null) ? activity : Utils.getContext();
        final Intent intent = new Intent(context, screen);
        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

}

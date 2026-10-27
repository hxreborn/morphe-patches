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

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class PatchContext {

    private static WeakReference<Activity> resumedActivityReference = new WeakReference<>(null);

    private PatchContext() {

    }

    public static Activity resumedActivity() {
        return resumedActivityReference.get();
    }

    public static void attach(Application application) {
        Utils.setContext(application);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            AccentOverlay.install(application);
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(Activity activity, Bundle bundle) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    AccentOverlay.applyTo(activity);
                }
            }

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

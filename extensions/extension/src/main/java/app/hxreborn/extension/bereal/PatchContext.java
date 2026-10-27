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

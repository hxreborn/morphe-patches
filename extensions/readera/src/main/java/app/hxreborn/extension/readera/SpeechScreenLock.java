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
package app.hxreborn.extension.readera;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

final class SpeechScreenLock {

    static final String TITLE = "Lock screen on Read chapter aloud";

    static final int ACTIVATION_REQUEST = 7401;

    static final String ADMIN_MISSING_NOTICE = "Screen not locked: device admin is off. Turn the lock on again in "
            + "the patches menu.";

    private static final String PREFERENCE = "lock_screen_while_reading_aloud";

    private static final String ACTIVATION_EXPLANATION = "Lets ReadEra lock the phone when Read chapter aloud "
            + "starts. The chapter keeps playing.";

    private SpeechScreenLock() {
    }

    static boolean applied() {
        return false;
    }

    static boolean enabled(Context context) {
        return PatchSettings.preferences(context).getBoolean(PREFERENCE, false);
    }

    static boolean active(Context context) {
        return enabled(context) && adminActive(context);
    }

    static void setEnabled(Context context, boolean enabled) {
        PatchSettings.preferences(context).edit().putBoolean(PREFERENCE, enabled).apply();
    }

    static void onSwitchToggled(Activity activity, boolean enabled) {
        if (!enabled) {
            removeAdmin(activity);
            return;
        }
        if (adminActive(activity)) {
            setEnabled(activity, true);
            return;
        }
        setEnabled(activity, false);
        Intent activate = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        activate.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(activity));
        activate.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, ACTIVATION_EXPLANATION);
        activity.startActivityForResult(activate, ACTIVATION_REQUEST);
    }

    static void onActivationResult(Context context) {
        setEnabled(context, adminActive(context));
    }

    static void removeAdmin(Context context) {
        setEnabled(context, false);
        if (adminActive(context)) {
            policy(context).removeActiveAdmin(adminComponent(context));
        }
    }

    static void onChapterSpeechStart(Context context) {
        if (!applied() || context == null || !enabled(context)) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> lockScreen(context));
    }

    private static void lockScreen(Context context) {
        if (!enabled(context)) {
            return;
        }
        if (!adminActive(context)) {
            Toast.makeText(context, ADMIN_MISSING_NOTICE, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            policy(context).lockNow();
        } catch (SecurityException ex) {
            Log.w(PatchSettings.LOG_TAG, "Device admin lockNow failed", ex);
            Toast.makeText(context, ADMIN_MISSING_NOTICE, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean adminActive(Context context) {
        DevicePolicyManager policy = policy(context);
        return policy != null && policy.isAdminActive(adminComponent(context));
    }

    private static DevicePolicyManager policy(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    private static ComponentName adminComponent(Context context) {
        return new ComponentName(context, ScreenLockAdmin.class);
    }

}

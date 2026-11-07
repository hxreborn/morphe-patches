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
package app.hxreborn.extension.xtreamplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class AndroidAutoInstaller {

    private static final String PACKAGE_INSTALLER = "com.google.android.packageinstaller";

    private static final String ANDROID_AUTO = "com.google.android.projection.gearhead";

    private static final String COMPATIBLE_INSTALLER = "com.android.vending";

    private static final String PREFERENCES = "hx_xtreamplayer_android_auto";

    private static final String DISMISSED_UPDATE_TIME = "dismissed_update_time";

    private static final String LOG_TAG = "hxXtreamPlayer";

    private static boolean promptShown;

    private final Activity activity;

    private final SharedPreferences preferences;

    private AndroidAutoInstaller(Activity activity) {
        this.activity = activity;
        this.preferences = activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public static void promptIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA || promptShown || activity.isFinishing()
                || activity.isDestroyed()) {
            return;
        }
        new AndroidAutoInstaller(activity).promptIfNeeded();
    }

    private void promptIfNeeded() {
        PackageManager packages = this.activity.getPackageManager();
        try {
            packages.getPackageInfo(PACKAGE_INSTALLER, 0);
            packages.getPackageInfo(ANDROID_AUTO, 0);
        } catch (PackageManager.NameNotFoundException ex) {
            return;
        }
        try {
            if (installedFromPlay(packages)) {
                return;
            }
            long updateTime = packages.getPackageInfo(this.activity.getPackageName(), 0).lastUpdateTime;
            if (this.preferences.getLong(DISMISSED_UPDATE_TIME, 0) == updateTime) {
                return;
            }
            promptShown = true;
            this.activity.getWindow().getDecorView().post(() -> showPrompt(updateTime));
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(LOG_TAG, "Could not read Android Auto installation state", ex);
        }
    }

    @TargetApi(Build.VERSION_CODES.BAKLAVA)
    private boolean installedFromPlay(PackageManager packages) throws PackageManager.NameNotFoundException {
        InstallSourceInfo source = packages.getInstallSourceInfo(this.activity.getPackageName());
        String installer = source.getInitiatingPackageName();
        if (PACKAGE_INSTALLER.equals(installer)) {
            installer = source.getInstallingPackageName();
        }
        return COMPATIBLE_INSTALLER.equals(installer);
    }

    private void showPrompt(long updateTime) {
        if (this.activity.isFinishing() || this.activity.isDestroyed()) {
            return;
        }
        AlertDialog prompt = new AlertDialog.Builder(this.activity).setTitle("Android Auto")
            .setMessage("Confirm app installation to show full Android Auto screen.")
            .setPositiveButton("Continue", (dialog, which) -> install())
            .setNegativeButton("Not now", (dialog, which) -> rememberDismissal(updateTime))
            .setOnCancelListener((dialog) -> rememberDismissal(updateTime))
            .show();
        TypedArray colors = this.activity.obtainStyledAttributes(new int[] { accentColorAttribute() });
        try {
            int accent = colors.getColor(0, prompt.getButton(AlertDialog.BUTTON_POSITIVE).getCurrentTextColor());
            for (int buttonId : new int[] { AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE }) {
                prompt.getButton(buttonId).setTextColor(accent);
            }
        } finally {
            colors.recycle();
        }
    }

    private static int accentColorAttribute() {
        return 0;
    }

    private void rememberDismissal(long updateTime) {
        this.preferences.edit().putLong(DISMISSED_UPDATE_TIME, updateTime).apply();
    }

    private void install() {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setDataAndType(SourceApkProvider.uri(this.activity), SourceApkProvider.APK_MIME_TYPE);
        intent.setPackage(PACKAGE_INSTALLER);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, COMPATIBLE_INSTALLER);
        try {
            this.activity.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException ex) {
            Log.w(LOG_TAG, "Could not open Android Auto installer", ex);
            Toast.makeText(this.activity, "Android Auto setup could not start.", Toast.LENGTH_LONG).show();
        }
    }

}

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
package app.hxreborn.extension.ultimea;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;

final class ThemeChoice {

    private static final int DARK = 1;

    private static final int AMOLED = 2;

    private static final CharSequence[] CHOICES = { "Dark", "AMOLED" };

    private final Activity activity;

    private final SettingsRows rows;

    ThemeChoice(Activity activity) {
        this.activity = activity;
        this.rows = new SettingsRows(activity, new SettingsStyle(activity));
    }

    void addTo(LinearLayout card) {
        int mode = ThemeSettings.getMode(this.activity);
        this.rows.actionRow(card, "hx_ultimea_ic_patches", "Theme", (mode == AMOLED) ? "AMOLED" : "Dark", this::show);
    }

    private void show() {
        new AlertDialog.Builder(this.activity, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Theme")
            .setSingleChoiceItems(CHOICES, ThemeSettings.getMode(this.activity) - DARK, (dialog, which) -> {
                int mode = which + DARK;
                dialog.dismiss();
                if (mode == ThemeSettings.getMode(this.activity)) {
                    return;
                }
                if (ThemeSettings.setMode(this.activity, mode)) {
                    confirmRestart();
                } else {
                    Toast.makeText(this.activity, "Could not save theme", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmRestart() {
        CharSequence appName = this.activity.getApplicationInfo().loadLabel(this.activity.getPackageManager());
        new AlertDialog.Builder(this.activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Restart required")
            .setMessage(appName + " must restart to apply the change.")
            .setPositiveButton("Restart", (dialog, which) -> restart())
            .setNegativeButton("Later", (dialog, which) -> this.activity.recreate())
            .show();
    }

    private void restart() {
        Intent launch = this.activity.getPackageManager().getLaunchIntentForPackage(this.activity.getPackageName());
        if (launch == null) {
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.activity.startActivity(launch);
        this.activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }

}

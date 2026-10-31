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
package app.hxreborn.extension.bereal.backup;

import java.text.DateFormat;
import java.util.Date;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class MemoriesBackup {

    private static final String DEFAULT_SUMMARY = "Saves every memory to the download folder as separate photos.";

    private MemoriesBackup() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(context, "Backup needs Android 10 or newer", Toast.LENGTH_LONG).show();
            return;
        }
        if (!BeRealApi.isReady()) {
            Toast.makeText(context, "Restart BeReal and try again", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && context instanceof Activity
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ((Activity) context).requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
        // The service runs either way, a denied permission only hides its notification
        context.startForegroundService(new Intent(context, BackupService.class));
    }

    public static String summary() {
        final String lastRun = PatchSettings.BACKUP_LAST_RUN.get();
        final String count = PatchSettings.BACKUP_LAST_COUNT.get();
        if (lastRun.isEmpty() || count.isEmpty()) {
            return DEFAULT_SUMMARY;
        }
        try {
            final String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(Long.parseLong(lastRun)));
            return "Last backup: " + date + ", " + count + " memories";
        } catch (NumberFormatException ex) {
            return DEFAULT_SUMMARY;
        }
    }

}

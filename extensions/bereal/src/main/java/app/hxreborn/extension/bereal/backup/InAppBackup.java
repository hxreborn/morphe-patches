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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import app.hxreborn.extension.shared.HostedComponents;

@SuppressLint("NotificationPermission")
final class InAppBackup implements BackupHost {

    private static final String THREAD_NAME = "hxBeRealBackup";

    private final NotificationManager manager;

    private InAppBackup(final Context context) {
        this.manager = context.getSystemService(NotificationManager.class);
    }

    static void start(final Context context) {
        BackupTask.createChannel(context);
        final Intent cancelIntent = HostedComponents.route(context, new Intent(context, BackupCancelReceiver.class));
        final PendingIntent cancelAction = PendingIntent.getBroadcast(context, 0, cancelIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        final BackupTask task = new BackupTask(context, new InAppBackup(context), cancelAction, false);
        if (!task.begin()) {
            Toast.makeText(context, "Backup already running", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(context, "Keep BeReal open until the backup finishes", Toast.LENGTH_LONG).show();
        new Thread(task::backUp, THREAD_NAME).start();
    }

    @Override
    public void showProgress(final Notification progress) {
        this.manager.notify(BackupTask.PROGRESS_ID, progress);
    }

    @Override
    public void finished() {
        this.manager.cancel(BackupTask.PROGRESS_ID);
    }

}

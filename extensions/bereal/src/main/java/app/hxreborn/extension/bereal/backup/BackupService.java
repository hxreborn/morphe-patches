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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

@SuppressLint({ "NotificationPermission", "UseRequiresApi", "ObsoleteSdkInt" })
@TargetApi(Build.VERSION_CODES.S_V2)
public final class BackupService extends Service implements BackupHost {

    static final String ACTION_CANCEL = "app.hxreborn.extension.bereal.backup.CANCEL";

    private ExecutorService executor;

    private BackupTask task;

    // Latest start id, so the finish only stops the service when no newer start is pending
    private int latestStartId;

    @Override
    public void onCreate() {
        BackupTask.createChannel(this);
        final Intent cancelIntent = new Intent(this, BackupService.class).setAction(ACTION_CANCEL);
        final PendingIntent cancelAction = PendingIntent.getService(this, 0, cancelIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        this.task = new BackupTask(this, this, cancelAction, true);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        this.latestStartId = startId;
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            if (BackupTask.isRunning()) {
                BackupTask.requestCancel();
            } else {
                stopSelf(startId);
            }
            return START_NOT_STICKY;
        }
        if (!this.task.begin()) {
            Toast.makeText(this, "Backup already running", Toast.LENGTH_LONG).show();
            showProgress(this.task.currentProgress());
            return START_NOT_STICKY;
        }
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor();
        }
        this.executor.execute(this.task::backUp);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    // Android 15 gives a dataSync service about six hours
    public void onTimeout(final int startId, final int fgsType) {
        BackupTask.requestCancel();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        // The run notices the cancel, then finishes and clears the running flag
        BackupTask.requestCancel();
        if (this.executor != null) {
            this.executor.shutdown();
        }
    }

    @Override
    public void showProgress(final Notification progress) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(BackupTask.PROGRESS_ID, progress, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(BackupTask.PROGRESS_ID, progress);
        }
    }

    @Override
    public void finished() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf(this.latestStartId);
    }

}

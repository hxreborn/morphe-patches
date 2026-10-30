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

import java.io.IOException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressLint("NotificationPermission")
@TargetApi(Build.VERSION_CODES.S_V2)
public final class BackupService extends Service {

    static final String ACTION_CANCEL = "app.hxreborn.extension.bereal.backup.CANCEL";

    private static final String TAG = "BackupService";

    private static final String CHANNEL_ID = "memories_backup";

    private static final int PROGRESS_ID = 4101;

    private static final int RESULT_ID = 4102;

    // Requests go out one every 500 ms (eg. 77 moments take about 40 seconds)
    private static final long REQUEST_DELAY_MILLIS = 500;

    private static final AtomicBoolean running = new AtomicBoolean();

    private static final AtomicBoolean cancel = new AtomicBoolean();

    private ExecutorService executor;

    private final Handler main = new Handler(Looper.getMainLooper());

    // Latest start id, so the finish only stops the service when no newer start is pending
    private int latestStartId;

    private volatile int lastDone;

    private volatile int lastTotal;

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager.class).createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, "Memories backup", NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        this.latestStartId = startId;
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            if (running.get()) {
                cancel.set(true);
            } else {
                stopSelf(startId);
            }
            return START_NOT_STICKY;
        }
        if (!running.compareAndSet(false, true)) {
            Toast.makeText(this, "Backup already running", Toast.LENGTH_LONG).show();
            startForeground(progress(this.lastDone, this.lastTotal));
            return START_NOT_STICKY;
        }
        this.lastDone = 0;
        this.lastTotal = 0;
        startForeground(progress(0, 0));
        cancel.set(false);
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor();
        }
        this.executor.execute(this::backUp);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    // Android 15 gives a dataSync service about six hours
    public void onTimeout(final int startId, final int fgsType) {
        cancel.set(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        // The run notices the cancel, then finishes and clears the running flag
        cancel.set(true);
        if (this.executor != null) {
            this.executor.shutdown();
        }
    }

    private void backUp() {
        BackupRun.Outcome outcome = new BackupRun.Outcome();
        try {
            final Ids ids = new Ids();
            final MemorySaver saver = new MemorySaver(this, BeRealApi::open, ZoneId.systemDefault());
            final MemoriesApi api = new MemoriesApi() {
                @Override
                public String memories() throws IOException {
                    return BeRealApi.memories();
                }

                @Override
                public String moment(final String momentId) throws IOException {
                    return BeRealApi.moment(momentId);
                }
            };
            try {
                outcome = new BackupRun(api, saver::save, ids, this::notifyProgress, REQUEST_DELAY_MILLIS)
                        .run(cancel);
            } finally {
                ids.flush();
            }
            if (outcome.saved > 0 || (!outcome.stoppedEarly && !outcome.cancelled)) {
                PatchSettings.BACKUP_LAST_RUN.save(String.valueOf(System.currentTimeMillis()));
                PatchSettings.BACKUP_LAST_COUNT.save(String.valueOf(ids.done().size()));
            }
        } catch (RuntimeException | OutOfMemoryError ex) {
            Log.e(TAG, "Backup failed", ex);
            outcome.reason = BackupRun.REASON_ERROR;
            outcome.stoppedEarly = true;
        } finally {
            final BackupRun.Outcome finished = outcome;
            this.main.post(() -> finish(finished));
        }
    }

    // On the main thread, so a tap cannot start a run between the reset and stopSelf
    private void finish(final BackupRun.Outcome outcome) {
        running.set(false);
        stopForeground(STOP_FOREGROUND_REMOVE);
        final Notification notification = result(outcome);
        final NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(RESULT_ID, notification);
        if (!manager.areNotificationsEnabled()) {
            Toast.makeText(this, notification.extras.getCharSequence(Notification.EXTRA_TEXT), Toast.LENGTH_LONG)
                    .show();
        }
        stopSelf(this.latestStartId);
    }

    private void startForeground(final Notification notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(PROGRESS_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(PROGRESS_ID, notification);
        }
    }

    private void notifyProgress(final int done, final int total) {
        this.lastDone = done;
        this.lastTotal = total;
        getSystemService(NotificationManager.class).notify(PROGRESS_ID, progress(done, total));
    }

    private Notification progress(final int done, final int total) {
        final Intent cancelIntent = new Intent(this, BackupService.class).setAction(ACTION_CANCEL);
        final PendingIntent cancelAction = PendingIntent.getService(this, 0, cancelIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return builder()
                .setContentTitle("Backing up memories")
                .setContentText((total > 0) ? done + " of " + total : "Starting")
                .setProgress(total, done, total == 0)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Cancel",
                        cancelAction).build())
                .build();
    }

    private Notification result(final BackupRun.Outcome outcome) {
        final String counts = "Saved " + outcome.saved + ", skipped " + outcome.skipped + ", failed "
                + outcome.failed;
        final String text;
        if (outcome.cancelled) {
            text = "Cancelled. " + counts;
        } else if (BackupRun.REASON_LOGIN.equals(outcome.reason)) {
            text = "Log in to BeReal again, then retry. " + counts;
        } else if (BackupRun.REASON_LIST.equals(outcome.reason)) {
            text = "Could not load your memories. Try again later.";
        } else if (BackupRun.REASON_ERROR.equals(outcome.reason)) {
            text = "Stopped by an unexpected error. " + counts;
        } else if (outcome.stoppedEarly) {
            text = "Stopped after repeated errors. " + counts;
        } else {
            text = counts;
        }
        return builder().setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Memories backup")
                .setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true)
                .build();
    }

    private Notification.Builder builder() {
        final Notification.Builder builder = (Build.VERSION.SDK_INT >= 26)
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.stat_sys_download);
    }

    private static final class Ids implements SavedMoments {

        private static final int SAVE_EVERY = 10;

        private final Set<String> ids = new LinkedHashSet<>();

        private int unsaved;

        private Ids() {
            // The first line is the download folder the ids were saved to
            final String[] saved = PatchSettings.BACKUP_SAVED_IDS.get().split("\n");
            if (saved.length > 0 && saved[0].equals(folderKey())) {
                this.ids.addAll(Arrays.asList(saved).subList(1, saved.length));
                this.ids.remove("");
            }
        }

        private static String folderKey() {
            return "folder:" + PatchSettings.DOWNLOAD_FOLDER.get();
        }

        @Override
        public Set<String> done() {
            return new LinkedHashSet<>(this.ids);
        }

        @Override
        public void mark(final String momentId) {
            this.ids.add(momentId);
            if (++this.unsaved >= SAVE_EVERY) {
                flush();
            }
        }

        private void flush() {
            if (this.unsaved > 0) {
                PatchSettings.BACKUP_SAVED_IDS.save(folderKey() + "\n" + String.join("\n", this.ids));
                this.unsaved = 0;
            }
        }
    }
}

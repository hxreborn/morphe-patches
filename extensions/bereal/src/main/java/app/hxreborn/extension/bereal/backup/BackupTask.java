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
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressLint({ "NotificationPermission", "UseRequiresApi", "ObsoleteSdkInt" })
@TargetApi(Build.VERSION_CODES.S_V2)
final class BackupTask {

    static final int PROGRESS_ID = 4101;

    private static final String TAG = "BackupTask";

    private static final String CHANNEL_ID = "memories_backup";

    private static final int RESULT_ID = 4102;

    // Requests go out one every 500 ms (eg. 77 moments take about 40 seconds)
    private static final long REQUEST_DELAY_MILLIS = 500;

    private static final AtomicBoolean running = new AtomicBoolean();

    private static final AtomicBoolean cancel = new AtomicBoolean();

    private final Context context;

    private final BackupHost host;

    private final PendingIntent cancelAction;

    private final boolean ongoing;

    private final Handler main = new Handler(Looper.getMainLooper());

    private volatile int lastDone;

    private volatile int lastTotal;

    BackupTask(final Context context, final BackupHost host, final PendingIntent cancelAction, final boolean ongoing) {
        this.context = context;
        this.host = host;
        this.cancelAction = cancelAction;
        this.ongoing = ongoing;
    }

    static void createChannel(final Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager.class)
                .createNotificationChannel(
                        new NotificationChannel(CHANNEL_ID, "Memories backup", NotificationManager.IMPORTANCE_LOW));
        }
    }

    static boolean isRunning() {
        return running.get();
    }

    static void requestCancel() {
        cancel.set(true);
    }

    boolean begin() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        this.lastDone = 0;
        this.lastTotal = 0;
        this.host.showProgress(progress(0, 0));
        cancel.set(false);
        return true;
    }

    Notification currentProgress() {
        return progress(this.lastDone, this.lastTotal);
    }

    void backUp() {
        BackupRun.Outcome outcome = new BackupRun.Outcome();
        try {
            final Ids ids = new Ids();
            final MemorySaver saver = new MemorySaver(this.context, BeRealApi::open, ZoneId.systemDefault());
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
                outcome = new BackupRun(api, saver::save, ids, this::notifyProgress, REQUEST_DELAY_MILLIS).run(cancel);
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
        final Notification notification = result(outcome);
        final NotificationManager manager = this.context.getSystemService(NotificationManager.class);
        manager.notify(RESULT_ID, notification);
        if (!manager.areNotificationsEnabled()) {
            Toast
                .makeText(this.context, notification.extras.getCharSequence(Notification.EXTRA_TEXT), Toast.LENGTH_LONG)
                .show();
        }
        this.host.finished();
    }

    private void notifyProgress(final int done, final int total) {
        this.lastDone = done;
        this.lastTotal = total;
        this.context.getSystemService(NotificationManager.class).notify(PROGRESS_ID, progress(done, total));
    }

    private Notification progress(final int done, final int total) {
        final Notification.Builder builder = builder().setContentTitle("Backing up memories")
            .setContentText((total > 0) ? done + " of " + total : "Starting")
            .setProgress(total, done, total == 0)
            .setOngoing(this.ongoing)
            .addAction(new Notification.Action.Builder(
                    Icon.createWithResource(this.context, android.R.drawable.ic_menu_close_clear_cancel), "Cancel",
                    this.cancelAction)
                .build());
        return builder.build();
    }

    private Notification result(final BackupRun.Outcome outcome) {
        final String counts = "Saved " + outcome.saved + ", skipped " + outcome.skipped + ", failed " + outcome.failed;
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
        return builder().setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Memories backup")
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build();
    }

    @SuppressWarnings("deprecation")
    private Notification.Builder builder() {
        final Notification.Builder builder = (Build.VERSION.SDK_INT >= 26)
                ? new Notification.Builder(this.context, CHANNEL_ID) : new Notification.Builder(this.context);
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

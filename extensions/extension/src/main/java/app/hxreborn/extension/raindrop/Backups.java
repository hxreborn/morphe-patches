/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

final class Backups {

    private static final String TAG = "RaindropPro";

    private static final String PREFERENCES = "hx_raindrop_backups";

    private static final String LAST_BACKUP_PREFIX = "last_backup_";

    private static final long INTERVAL_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private static final String FOLDER = Environment.DIRECTORY_DOWNLOADS + "/Raindrop";

    private static final String EXPORT_HEADER = "<!DOCTYPE NETSCAPE-Bookmark-file-1>";

    private static final AtomicBoolean running = new AtomicBoolean();

    private Backups() {
    }

    static void backUpIfDue(Context context, final long account) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        final ContentResolver resolver = context.getContentResolver();
        final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        final String lastBackupKey = LAST_BACKUP_PREFIX + account;
        if (System.currentTimeMillis() - preferences.getLong(lastBackupKey, 0) < INTERVAL_MILLIS
                || !running.compareAndSet(false, true)) {
            return;
        }
        new Thread(() -> {
            try {
                byte[] export = RaindropApi.getBytes("raindrops/0/export.html");
                if (!new String(export, 0, Math.min(export.length, EXPORT_HEADER.length()), StandardCharsets.UTF_8)
                    .equals(EXPORT_HEADER)) {
                    throw new IOException("Export is not a bookmark file (" + export.length + " bytes)");
                }
                save(resolver, export);
                preferences.edit().putLong(lastBackupKey, System.currentTimeMillis()).apply();
            } catch (IOException | RuntimeException ex) {
                Log.w(TAG, "Backup failed", ex);
            } finally {
                running.set(false);
            }
        }, "RaindropBackup").start();
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private static void save(ContentResolver resolver, byte[] export) throws IOException {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "raindrop-backup-" + date + ".html");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/html");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("MediaStore refused " + values);
        }
        try {
            try (OutputStream output = resolver.openOutputStream(uri)) {
                if (output == null) {
                    throw new IOException("No output stream for " + uri);
                }
                output.write(export);
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, published, null, null);
        } catch (IOException | RuntimeException ex) {
            resolver.delete(uri, null, null);
            throw ex;
        }
    }

}

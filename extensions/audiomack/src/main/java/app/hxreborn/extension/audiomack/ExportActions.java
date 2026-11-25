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
package app.hxreborn.extension.audiomack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class ExportActions {

    static final String LOG_TAG = "hxAudiomack";

    private static final String SAVE_LABEL = "hx_audiomack_save_to_device";

    private static final String SHARE_LABEL = "hx_audiomack_share_file";

    private static final String SAVED_MESSAGE = "Saved to " + SongExport.FOLDER;

    private static final String FAILED_MESSAGE = "Couldn't save the song";

    private static final ExecutorService EXPORT_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    private static volatile Context appContext;

    private ExportActions() {
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void addRows(List<Object> rows, Object music, boolean downloaded) {
        if (!downloaded || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || appContext == null) {
            return;
        }
        final File file = downloadedFile(music);
        if (file == null || !file.isFile()) {
            return;
        }
        rows.add(new ExportRow(saveIcon(), music, false));
        rows.add(new ExportRow(shareIcon(), music, true));
    }

    public static boolean onRowClick(Object row) {
        if (!(row instanceof ExportRow)) {
            return false;
        }
        final ExportRow exportRow = (ExportRow) row;
        EXPORT_EXECUTOR.execute(() -> export(exportRow));
        return true;
    }

    @SuppressLint("DiscouragedApi")
    static int labelResource(boolean share) {
        final Context context = appContext;
        return context.getResources()
            .getIdentifier(share ? SHARE_LABEL : SAVE_LABEL, "string", context.getPackageName());
    }

    private static void export(ExportRow row) {
        final Context context = appContext;
        final File file = downloadedFile(row.music);
        final SongTags tags = new SongTags(musicId(row.music), musicTitle(row.music), musicArtist(row.music),
                musicAlbum(row.music));
        try {
            final SavedSong song = new SongExport(context).save(file, tags);
            if (row.share) {
                share(context, song, tags);
            } else {
                toast(context, SAVED_MESSAGE);
            }
        } catch (IOException | RuntimeException ex) {
            Log.e(LOG_TAG, "Downloaded song not exported", ex);
            toast(context, FAILED_MESSAGE);
        }
    }

    private static void share(Context context, SavedSong song, SongTags tags) {
        final Intent send = new Intent(Intent.ACTION_SEND).setType(song.mimeType)
            .putExtra(Intent.EXTRA_STREAM, song.uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri(tags.fileName(), song.uri));
        final Intent chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MAIN_THREAD.post(() -> context.startActivity(chooser));
    }

    private static void toast(Context context, String message) {
        MAIN_THREAD.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private static File downloadedFile(Object music) {
        final String id = musicId(music);
        final File root = appContext.getExternalFilesDir(null);
        if (id == null || id.isEmpty() || root == null) {
            return null;
        }
        return new File(new File(root, downloadFolderName()), id);
    }

    static String musicId(Object music) {
        return null;
    }

    static String musicTitle(Object music) {
        return null;
    }

    static String musicArtist(Object music) {
        return null;
    }

    static String musicAlbum(Object music) {
        return null;
    }

    static String downloadFolderName() {
        return null;
    }

    static int saveIcon() {
        return 0;
    }

    static int shareIcon() {
        return 0;
    }

}

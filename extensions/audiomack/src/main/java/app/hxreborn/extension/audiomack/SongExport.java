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
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

@TargetApi(Build.VERSION_CODES.Q)
final class SongExport {

    static final String FOLDER = Environment.DIRECTORY_MUSIC + "/Audiomack";

    private static final String SAME_FILE_SELECTION = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND "
            + MediaStore.MediaColumns.DISPLAY_NAME + "=?";

    private final ContentResolver resolver;

    SongExport(Context context) {
        this.resolver = context.getContentResolver();
    }

    SavedSong save(File source, SongTags tags) throws IOException {
        final AudioFormat format = AudioFormat.of(source);
        final String name = tags.fileName() + format.extension;
        final Uri collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        final Uri existing = find(collection, name);
        if (existing != null) {
            return new SavedSong(existing, format.mimeType);
        }
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        final Uri item = this.resolver.insert(collection, values);
        if (item == null) {
            throw new IOException("MediaStore did not create the song file");
        }
        try (OutputStream out = this.resolver.openOutputStream(item)) {
            if (out == null) {
                throw new IOException("MediaStore did not open the song file");
            }
            format.writeTagged(source, out, tags);
        } catch (IOException | RuntimeException ex) {
            this.resolver.delete(item, null, null);
            throw ex;
        }
        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        this.resolver.update(item, values, null, null);
        return new SavedSong(item, format.mimeType);
    }

    private Uri find(Uri collection, String name) {
        final String[] projection = { MediaStore.MediaColumns._ID };
        final String[] arguments = { FOLDER + "/", name };
        try (Cursor cursor = this.resolver.query(collection, projection, SAME_FILE_SELECTION, arguments, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            return ContentUris.withAppendedId(collection, cursor.getLong(0));
        }
    }

}

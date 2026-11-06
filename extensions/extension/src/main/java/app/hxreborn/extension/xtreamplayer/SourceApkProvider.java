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

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

@SuppressWarnings("unused")
public final class SourceApkProvider extends ContentProvider {

    static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    private static final String AUTHORITY_SUFFIX = ".hxreborn.installer";

    private static final String APK_DISPLAY_NAME = "XtreamPlayer.apk";

    private static final String APK_PATH = "/base.apk";

    @Override
    public boolean onCreate() {
        return true;
    }

    static Uri uri(Context context) {
        return new Uri.Builder().scheme("content")
            .authority(context.getPackageName() + AUTHORITY_SUFFIX)
            .path(APK_PATH)
            .build();
    }

    @Override
    public String getType(Uri uri) {
        return APK_MIME_TYPE;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("APK provider only permits reading");
        }
        return ParcelFileDescriptor.open(sourceApk(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final File file;
        try {
            file = sourceApk(uri);
        } catch (FileNotFoundException ex) {
            return null;
        }
        String[] columns = (projection != null) ? projection
                : new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
        MatrixCursor cursor = new MatrixCursor(columns);
        Object[] values = new Object[columns.length];
        for (int index = 0; index < columns.length; index++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[index])) {
                values[index] = APK_DISPLAY_NAME;
            } else if (OpenableColumns.SIZE.equals(columns[index])) {
                values[index] = file.length();
            }
        }
        cursor.addRow(values);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("APK provider only permits reading");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("APK provider only permits reading");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("APK provider only permits reading");
    }

    private File sourceApk(Uri uri) throws FileNotFoundException {
        Context context = getContext();
        if (context == null || !APK_PATH.equals(uri.getPath())
                || !(context.getPackageName() + AUTHORITY_SUFFIX).equals(uri.getAuthority()) || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new FileNotFoundException("Unknown APK provider path");
        }
        return new File(context.getApplicationInfo().sourceDir);
    }

}

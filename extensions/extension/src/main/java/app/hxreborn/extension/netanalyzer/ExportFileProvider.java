/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

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
public final class ExportFileProvider extends ContentProvider {

    static final String CSV_MIME = "text/csv";

    private static final String AUTHORITY_SUFFIX = ".netanalyzer-an.provider";

    private static final String DIRECTORY = "export";

    private static final String CSV_FILE = "export.csv";

    @Override
    public boolean onCreate() {
        return true;
    }

    static Uri uri(Context context) {
        return new Uri.Builder().scheme("content")
            .authority(context.getPackageName() + AUTHORITY_SUFFIX)
            .appendPath(CSV_FILE)
            .build();
    }

    static File file(Context context) {
        return new File(new File(context.getCacheDir(), DIRECTORY), CSV_FILE);
    }

    @Override
    public String getType(Uri uri) {
        return CSV_MIME;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(exportedFile(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final File file;
        try {
            file = exportedFile(uri);
        } catch (FileNotFoundException ex) {
            return null;
        }
        final MatrixCursor cursor = new MatrixCursor(
                new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE });
        cursor.addRow(new Object[] { file.getName(), file.length() });
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only export provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only export provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only export provider");
    }

    private File exportedFile(Uri uri) throws FileNotFoundException {
        if (!CSV_FILE.equals(uri.getLastPathSegment())) {
            throw new FileNotFoundException(uri.toString());
        }
        return file(getContext());
    }

}

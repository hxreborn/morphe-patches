/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.allvideoplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

@SuppressWarnings("unused")
public final class MediaStoreVideoUri {

    private static final String TAG = "MediaStoreVideoUri";

    private static final String EXTERNAL_STORAGE_DOCUMENTS = "com.android.externalstorage.documents";

    private static final String PRIMARY_VOLUME_PREFIX = "primary:";

    private MediaStoreVideoUri() {

    }

    public static Uri resolve(Activity activity, Uri uri) {
        if (uri == null || MediaStore.AUTHORITY.equals(uri.getAuthority())) {
            return uri;
        }

        try {
            Uri mediaUri = lookUp(activity.getContentResolver(), uri);
            if (mediaUri == null) {
                return uri;
            }

            Intent intent = activity.getIntent();
            if (intent != null && uri.equals(intent.getData())) {
                intent.setDataAndType(mediaUri, intent.getType());
            }
            return mediaUri;
        } catch (Exception exception) {
            Log.e(TAG, "Could not resolve " + uri, exception);
            return uri;
        }
    }

    private static Uri lookUp(ContentResolver resolver, Uri uri) {
        String path = filePath(resolver, uri);
        if (path != null) {
            Uri byPath = findVideo(resolver, MediaStore.MediaColumns.DATA + "=?", new String[] { path });
            if (byPath != null) {
                return byPath;
            }
        }

        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return null;
        }
        try (Cursor cursor = resolver.query(uri, new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE },
                null, null, null)) {
            if (cursor == null || !cursor.moveToFirst() || cursor.isNull(0) || cursor.isNull(1)) {
                return null;
            }
            return findVideo(resolver,
                    MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.SIZE + "=?",
                    new String[] { cursor.getString(0), String.valueOf(cursor.getLong(1)) });
        }
    }

    private static String filePath(ContentResolver resolver, Uri uri) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return uri.getPath();
        }
        if (EXTERNAL_STORAGE_DOCUMENTS.equals(uri.getAuthority())) {
            String documentId = uri.getLastPathSegment();
            if (documentId != null && documentId.startsWith(PRIMARY_VOLUME_PREFIX)) {
                return Environment.getExternalStorageDirectory() + "/"
                        + documentId.substring(PRIMARY_VOLUME_PREFIX.length());
            }
        }
        return null;
    }

    private static Uri findVideo(ContentResolver resolver, String selection, String[] selectionArgs) {
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = resolver.query(collection, new String[] { MediaStore.MediaColumns._ID }, selection,
                selectionArgs, null)) {
            if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
                return null;
            }
            return ContentUris.withAppendedId(collection, cursor.getLong(0));
        }
    }

}

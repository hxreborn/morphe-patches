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
package app.hxreborn.extension.bereal.download;

import java.io.FileNotFoundException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class DownloadFolder {

    public static final String PICTURES_FOLDER = Environment.DIRECTORY_PICTURES + "/BeReal";

    public static final String DCIM_FOLDER = Environment.DIRECTORY_DCIM + "/BeReal";

    public static final String INTERNAL_STORAGE = "Internal storage";

    private static final String TAG = "DownloadFolder";

    private static final String TREE_SCHEME = "content:";

    private static final String PRIMARY_VOLUME = "primary";

    private static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";

    private static final int READ_WRITE = Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private DownloadFolder() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static boolean isCustom() {
        return chosenTree() != null;
    }

    public static boolean isCustomAvailable(ContentResolver resolver) {
        final Uri tree = chosenTree();
        return tree != null && isAvailable(resolver, tree);
    }

    public static String presetFolder() {
        return DCIM_FOLDER.equals(PatchSettings.DOWNLOAD_FOLDER.get()) ? DCIM_FOLDER : PICTURES_FOLDER;
    }

    public static String label() {
        final Uri tree = chosenTree();
        if (tree == null) {
            return presetFolder();
        }
        final String documentId = DocumentsContract.getTreeDocumentId(tree);
        final String path = documentId.substring(documentId.indexOf(':') + 1);
        return path.isEmpty() ? volume() : path;
    }

    public static String volume() {
        final Uri tree = chosenTree();
        if (tree == null) {
            return INTERNAL_STORAGE;
        }
        final String documentId = DocumentsContract.getTreeDocumentId(tree);
        final int colon = documentId.indexOf(':');
        final String volume = (colon > 0) ? documentId.substring(0, colon) : documentId;
        return (isExternalStorage(tree) && PRIMARY_VOLUME.equals(volume)) ? INTERNAL_STORAGE : volume;
    }

    public static void releaseGrantIfUnused(ContentResolver resolver, String folder) {
        if (folder.startsWith(TREE_SCHEME) && !folder.equals(PatchSettings.DOWNLOAD_FOLDER.get())) {
            releaseGrant(resolver, Uri.parse(folder));
        }
    }

    public static Intent pickerIntent() {
        return new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(READ_WRITE | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    }

    public static void choose(ContentResolver resolver, Uri tree) {
        resolver.takePersistableUriPermission(tree, READ_WRITE);
        PatchSettings.DOWNLOAD_FOLDER.save(tree.toString());
    }

    public static void usePreset(String folder) {
        if (DCIM_FOLDER.equals(folder)) {
            PatchSettings.DOWNLOAD_FOLDER.save(DCIM_FOLDER);
        } else {
            PatchSettings.DOWNLOAD_FOLDER.resetToDefault();
        }
    }

    public static Uri insert(ContentResolver resolver, Uri collection, ContentValues values) {
        final String presetPath = presetFolder();
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, presetPath);
        final Uri tree = chosenTree();
        if (tree == null) {
            return resolver.insert(collection, values);
        }
        if (!isAvailable(resolver, tree)) {
            Log.e(TAG, "Download folder unavailable: " + tree);
            return resolver.insert(collection, values);
        }
        final String folderPath = primaryVolumePath(tree);
        if (folderPath != null) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, folderPath);
            try {
                final Uri inserted = resolver.insert(collection, values);
                if (inserted != null) {
                    return inserted;
                }
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "MediaStore rejected " + folderPath, ex);
            }
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, presetPath);
        }
        final Uri document = createDocument(resolver, tree, values);
        return (document != null) ? document : resolver.insert(collection, values);
    }

    private static Uri createDocument(ContentResolver resolver, Uri tree, ContentValues values) {
        try {
            return DocumentsContract.createDocument(resolver, folderDocument(tree),
                    values.getAsString(MediaStore.MediaColumns.MIME_TYPE),
                    values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME));
        } catch (FileNotFoundException | RuntimeException ex) {
            Log.e(TAG, "Download not created in " + tree, ex);
            return null;
        }
    }

    private static void releaseGrant(ContentResolver resolver, Uri tree) {
        try {
            resolver.releasePersistableUriPermission(tree, READ_WRITE);
        } catch (SecurityException ex) {
            Log.e(TAG, "Download folder grant not released: " + tree, ex);
        }
    }

    private static boolean isAvailable(ContentResolver resolver, Uri tree) {
        return hasWriteGrant(resolver, tree) && exists(resolver, tree);
    }

    private static boolean hasWriteGrant(ContentResolver resolver, Uri tree) {
        for (UriPermission permission : resolver.getPersistedUriPermissions()) {
            if (permission.getUri().equals(tree) && permission.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    private static boolean exists(ContentResolver resolver, Uri tree) {
        try (Cursor folder = resolver.query(folderDocument(tree),
                new String[] { DocumentsContract.Document.COLUMN_DOCUMENT_ID }, null, null, null)) {
            return folder != null && folder.moveToFirst();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isExternalStorage(Uri tree) {
        return EXTERNAL_STORAGE_AUTHORITY.equals(tree.getAuthority());
    }

    private static String primaryVolumePath(Uri tree) {
        if (!isExternalStorage(tree)) {
            return null;
        }
        final String documentId = DocumentsContract.getTreeDocumentId(tree);
        final String prefix = PRIMARY_VOLUME + ":";
        if (!documentId.startsWith(prefix) || documentId.length() == prefix.length()) {
            return null;
        }
        return documentId.substring(prefix.length());
    }

    private static Uri folderDocument(Uri tree) {
        return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
    }

    private static Uri chosenTree() {
        final String saved = PatchSettings.DOWNLOAD_FOLDER.get();
        return saved.startsWith(TREE_SCHEME) ? Uri.parse(saved) : null;
    }

}

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
package app.hxreborn.extension.ultimea.equalizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import org.json.JSONException;
import org.json.JSONObject;

public final class PresetShareProvider extends ContentProvider {

    private static final String AUTHORITY_SUFFIX = ".hx_equalizer_presets";

    private static final String DIRECTORY = "equalizer-presets";

    private static final String MIME_TYPE = "application/json";

    private static final int BUFFER_BYTES = 4096;

    private static final String COLLECTION_FORMAT = "ultimea-equalizer-presets";

    private static final String COLLECTION_FILENAME = "ULTIMEA presets.json";

    static void share(Activity activity, EqualizerPreset preset) throws IOException, JSONException {
        shareDocument(activity, EqualizerPresetFile.json(preset).toString(2), "Share preset");
    }

    static void shareAll(Activity activity, List<EqualizerPreset> presets) throws IOException, JSONException {
        shareDocument(activity, EqualizerPresetBundleFile.json(presets).toString(2), "Share presets");
    }

    private static void shareDocument(Activity activity, String content, String title) throws IOException {
        byte[] document = content.getBytes(StandardCharsets.UTF_8);
        if (document.length > EqualizerPresetBundleFile.MAX_BYTES) {
            throw new IOException("Preset file exceeds size limit");
        }
        File directory = new File(activity.getCacheDir(), DIRECTORY);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Preset share folder could not be created");
        }
        StringBuilder digest = new StringBuilder();
        try {
            for (byte value : MessageDigest.getInstance("SHA-256").digest(document)) {
                digest.append(String.format(Locale.ROOT, "%02x", value & 255));
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Preset share digest could not be created", ex);
        }
        File file = new File(directory, digest + ".json");
        if (!file.exists()) {
            File pending = new File(directory, digest + ".tmp");
            try (FileOutputStream output = new FileOutputStream(pending)) {
                output.write(document);
            }
            if (!pending.renameTo(file)) {
                throw new IOException("Preset share file could not be published");
            }
        }
        Uri uri = new Uri.Builder().scheme("content")
            .authority(activity.getPackageName() + AUTHORITY_SUFFIX)
            .appendPath(file.getName())
            .build();
        Intent send = new Intent(Intent.ACTION_SEND).setType(MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri(title, uri));
        Intent chooser = Intent.createChooser(send, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        chooser.setClipData(send.getClipData());
        activity.startActivity(chooser);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return MIME_TYPE;
    }

    private File file(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[0-9a-f]{64}\\.json") || !"content".equals(uri.getScheme())
                || !(getContext().getPackageName() + AUTHORITY_SUFFIX).equals(uri.getAuthority())
                || uri.getPathSegments().size() != 1) {
            throw new FileNotFoundException("Invalid preset share URI");
        }
        File file = new File(new File(getContext().getCacheDir(), DIRECTORY), name);
        if (!file.isFile()) {
            throw new FileNotFoundException("Shared preset was removed");
        }
        return file;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Preset shares are read-only");
        }
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = file(uri);
            String[] columns = (projection != null) ? projection
                    : new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
            MatrixCursor cursor = new MatrixCursor(columns);
            if (file.length() > EqualizerPresetBundleFile.MAX_BYTES) {
                return null;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_BYTES];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
            }
            String document = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            List<EqualizerPresetFile> presets = EqualizerPresetBundleFile.read(document);
            String name = COLLECTION_FORMAT.equals(new JSONObject(document).get("format")) ? COLLECTION_FILENAME
                    : presets.get(0).name.replace('/', '_').replace('\\', '_') + ".json";
            Object[] values = new Object[columns.length];
            for (int index = 0; index < columns.length; index++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[index])) {
                    values[index] = name;
                } else if (OpenableColumns.SIZE.equals(columns[index])) {
                    values[index] = file.length();
                }
            }
            cursor.addRow(values);
            return cursor;
        } catch (IOException | JSONException ex) {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Preset shares are read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Preset shares are read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Preset shares are read-only");
    }

}

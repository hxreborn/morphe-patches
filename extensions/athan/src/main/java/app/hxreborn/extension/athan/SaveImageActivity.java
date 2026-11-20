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
package app.hxreborn.extension.athan;

import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class SaveImageActivity extends Activity {

    private static final String TAG = "AthanSaveImage";

    private static final String ALBUM = "Athan";

    private static final String SAVE_LABEL = "Save to gallery";

    public static Intent createChooser(Intent target, CharSequence title) {
        Intent chooser = Intent.createChooser(target, title);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || target == null) {
            return chooser;
        }
        Uri image = sharedImage(target);
        Context context = application();
        if (image == null || context == null) {
            return chooser;
        }
        Intent save = new Intent(context, SaveImageActivity.class).setDataAndType(image, target.getType())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        LabeledIntent option = new LabeledIntent(save, "android", SAVE_LABEL, android.R.drawable.ic_menu_save);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { option });
        return chooser;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        boolean saved = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && save(getIntent());
        Toast.makeText(context, saved ? "Saved to Pictures/" + ALBUM : "Could not save the image", Toast.LENGTH_SHORT)
            .show();
        finish();
    }

    private static Uri sharedImage(Intent target) {
        String type = target.getType();
        if (type == null || !type.startsWith("image/")) {
            return null;
        }
        Object stream = target.getParcelableExtra(Intent.EXTRA_STREAM);
        return (stream instanceof Uri) ? (Uri) stream : null;
    }

    @SuppressLint({ "PrivateApi", "DiscouragedPrivateApi" })
    private static Context application() {
        try {
            return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "No application context", ex);
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private boolean save(Intent intent) {
        Uri image = intent.getData();
        if (image == null) {
            return false;
        }
        String type = (intent.getType() != null) ? intent.getType() : "image/jpeg";
        String extension = "image/png".equals(type) ? ".png" : ".jpg";
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, ALBUM + "_" + System.currentTimeMillis() + extension);
        values.put(MediaStore.MediaColumns.MIME_TYPE, type);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri destination = null;
        try {
            destination = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (destination == null) {
                return false;
            }
            try (InputStream input = resolver.openInputStream(image);
                    OutputStream output = resolver.openOutputStream(destination)) {
                if (input == null || output == null) {
                    throw new IllegalStateException("Cannot open " + image + " or " + destination);
                }
                byte[] buffer = new byte[16384];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(destination, published, null, null);
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Could not save " + image, ex);
            if (destination != null) {
                resolver.delete(destination, null, null);
            }
            return false;
        }
    }

}

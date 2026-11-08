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
package app.hxreborn.extension.ultimea;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused")
public final class NativeSettings {

    private static final String LIBRARY_ENTRY = "lib/arm64-v8a/libapp.so";

    private static final String LIBRARY_ARGUMENT = "--aot-shared-library-name=";

    private static final String LIBRARY_FILE = "hx_ultimea_libapp.so";

    private static final int COPY_BUFFER_BYTES = 65536;

    private NativeSettings() {
    }

    public static String[] prepare(Context context, String[] arguments) {
        AccentColor.prepare(context);
        if (!PatchSettings.removeDistractions() && !PatchSettings.removePopups()
                && !PatchSettings.stopFirmwareReminders()) {
            return arguments;
        }
        File output = null;
        try {
            output = File.createTempFile(LIBRARY_FILE, null, context.getCodeCacheDir());
            try (ZipFile apk = new ZipFile(context.getApplicationInfo().sourceDir)) {
                ZipEntry entry = apk.getEntry(LIBRARY_ENTRY);
                if (entry == null) {
                    throw new IOException("Native settings library is missing from APK");
                }
                try (InputStream input = apk.getInputStream(entry);
                        FileOutputStream file = new FileOutputStream(output)) {
                    byte[] buffer = new byte[COPY_BUFFER_BYTES];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        file.write(buffer, 0, count);
                    }
                }
            }
            try (RandomAccessFile library = new RandomAccessFile(output, "rw")) {
                if (PatchSettings.removeDistractions()) {
                    NativeEdits.apply(context.getAssets().open("hx_ultimea/" + PatchSettings.REMOVE_DISTRACTIONS),
                            library);
                }
                if (PatchSettings.removePopups()) {
                    NativeEdits.apply(context.getAssets().open("hx_ultimea/" + PatchSettings.REMOVE_POPUPS), library);
                }
                if (PatchSettings.stopFirmwareReminders()) {
                    NativeEdits.apply(context.getAssets().open("hx_ultimea/" + PatchSettings.STOP_FIRMWARE_REMINDERS),
                            library);
                }
            }
            if (!output.setReadOnly()) {
                throw new IOException("Native settings library could not be made read-only");
            }
            File published = new File(context.getCodeCacheDir(), LIBRARY_FILE);
            if (!output.renameTo(published)) {
                throw new IOException("Native settings library could not be published");
            }
            String[] result = arguments.clone();
            boolean replaced = false;
            for (int index = 0; index < result.length; index++) {
                if (result[index].startsWith(LIBRARY_ARGUMENT)) {
                    result[index] = LIBRARY_ARGUMENT + published.getAbsolutePath();
                    replaced = true;
                }
            }
            if (!replaced) {
                throw new IOException("Flutter native library argument is missing");
            }
            return result;
        } catch (IOException ex) {
            Log.e(PatchSettings.LOG_TAG, "Native settings preparation failed", ex);
            return arguments;
        } finally {
            if (output != null && output.exists() && !output.delete()) {
                Log.w(PatchSettings.LOG_TAG, "Temporary native settings library could not be removed");
            }
        }
    }

}

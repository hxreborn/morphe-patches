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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused")
public final class AccentColor {

    private static final String PALETTE_ASSET = "hx_ultimea/accent_colors";

    private static final String PALETTE_FILE = "hx_ultimea_accent";

    private static final int PALETTE_MAGIC = 0x31434148;

    private static final int HEADER_BYTES = 8;

    private static final int MAX_COLORS = 256;

    private static final int COLOR_BYTES = 4;

    private static final int PALETTE_RECORD_BYTES = 16;

    private static final int PUBLICATION_BYTES = 8;

    private static final String VERSION_FILE = "hx_ultimea_accent_version";

    private static final float CHANNEL_MAX = 255f;

    private static final int RGB_MASK = 255;

    private static final int RED_SHIFT = 16;

    private static final int GREEN_SHIFT = 8;

    private static final int MAX_ASSET_BYTES = HEADER_BYTES + MAX_COLORS * COLOR_BYTES;

    private static AccentObserver observer;

    private static final int LIGHT_PRIMARY = 0xff541597;

    private static final int DARK_PRIMARY = 0xffc499ff;

    private static final int LIGHT_SURFACE = 0xffeee5f7;

    private static final int DARK_SURFACE = 0xff332343;

    private AccentColor() {
    }

    public static boolean isPatched() {
        return AppliedPatches.accentColor();
    }

    public static int primary(Context context, boolean dark) {
        return isPatched() ? AccentPalette.primary(context, dark) : dark ? DARK_PRIMARY : LIGHT_PRIMARY;
    }

    public static int surface(Context context, boolean dark) {
        return isPatched() ? AccentPalette.surface(context, dark) : dark ? DARK_SURFACE : LIGHT_SURFACE;
    }

    public static int transform(Context context, int original) {
        return isPatched() ? AccentPalette.transform(context, original) : original;
    }

    public static void attach(Activity activity) {
        if (!isPatched()) {
            return;
        }
        if (observer == null) {
            observer = new AccentObserver(activity.getApplication());
        }
        observer.attach(activity);
    }

    static boolean setPreset(Context context, String preset) {
        String previous = AccentPalette.getPreset(context);
        if (!AccentPalette.setPreset(context, preset)) {
            if (!AccentPalette.setPreset(context, previous)) {
                Log.e(PatchSettings.LOG_TAG, "Previous accent selection could not be restored");
            }
            return false;
        }
        if (prepare(context)) {
            if (observer != null) {
                observer.changed();
            }
            return true;
        }
        boolean restoredSelection = AccentPalette.setPreset(context, previous);
        boolean restoredPalette = prepare(context);
        if (!restoredSelection || !restoredPalette) {
            Log.e(PatchSettings.LOG_TAG, "Previous accent selection could not be restored");
        }
        return false;
    }

    public static synchronized boolean prepare(Context context) {
        if (!isPatched()) {
            return true;
        }
        File replacement = null;
        try {
            byte[] source = readSources(context);
            ByteBuffer colors = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN);
            if (source.length < HEADER_BYTES || colors.getInt() != PALETTE_MAGIC) {
                throw new IOException("Invalid accent palette header");
            }
            int count = colors.getInt();
            if (count < 1 || count > MAX_COLORS || colors.remaining() != count * COLOR_BYTES) {
                throw new IOException("Invalid accent palette size");
            }
            ByteBuffer palette = ByteBuffer.allocate(count * PALETTE_RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
            for (int index = 0; index < count; index++) {
                int color = transform(context, colors.getInt());
                palette.putInt(color);
                palette.putFloat(((color >>> RED_SHIFT) & RGB_MASK) / CHANNEL_MAX);
                palette.putFloat(((color >>> GREEN_SHIFT) & RGB_MASK) / CHANNEL_MAX);
                palette.putFloat((color & RGB_MASK) / CHANNEL_MAX);
            }
            File published = new File(context.getFilesDir(), PALETTE_FILE);
            long version = 0;
            boolean unchanged = false;
            if (published.isFile() && published.length() == PUBLICATION_BYTES + palette.capacity()) {
                try (RandomAccessFile previous = new RandomAccessFile(published, "r")) {
                    byte[] bytes = new byte[PUBLICATION_BYTES + palette.capacity()];
                    previous.readFully(bytes);
                    ByteBuffer saved = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                    version = saved.getLong();
                    unchanged = java.util.Arrays.equals(palette.array(),
                            java.util.Arrays.copyOfRange(bytes, PUBLICATION_BYTES, bytes.length));
                }
            }
            if (!unchanged) {
                version++;
                byte[] publication = ByteBuffer.allocate(PUBLICATION_BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putLong(version)
                    .array();
                replacement = File.createTempFile(PALETTE_FILE, null, context.getFilesDir());
                try (FileOutputStream output = new FileOutputStream(replacement)) {
                    output.write(publication);
                    output.write(palette.array());
                    output.getFD().sync();
                }
                if (!replacement.renameTo(published)) {
                    throw new IOException("Accent palette could not be published");
                }
            }
            try (RandomAccessFile marker = new RandomAccessFile(new File(context.getFilesDir(), VERSION_FILE), "rw")) {
                marker.write(
                        ByteBuffer.allocate(PUBLICATION_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(version).array());
                marker.setLength(PUBLICATION_BYTES);
                marker.getFD().sync();
            }
            return true;
        } catch (IOException ex) {
            Log.e(PatchSettings.LOG_TAG, "Accent palette could not be prepared", ex);
            return false;
        } finally {
            if (replacement != null && replacement.exists() && !replacement.delete()) {
                Log.w(PatchSettings.LOG_TAG, "Temporary accent palette could not be removed");
            }
        }
    }

    private static byte[] readSources(Context context) throws IOException {
        try (InputStream source = context.getAssets().open(PALETTE_ASSET);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[MAX_ASSET_BYTES];
            int count;
            while ((count = source.read(buffer)) != -1) {
                if (bytes.size() > MAX_ASSET_BYTES - count) {
                    throw new IOException("Accent palette asset is too large");
                }
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        }
    }

}

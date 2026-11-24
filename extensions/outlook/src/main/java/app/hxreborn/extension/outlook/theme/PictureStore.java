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
package app.hxreborn.extension.outlook.theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import app.hxreborn.extension.outlook.settings.PatchSettings;

@TargetApi(Build.VERSION_CODES.P)
final class PictureStore {

    private static final String DIRECTORY = "hx_custom_theme";

    private static final String FILE_PREFIX = "picture_";

    private static final String FILE_SUFFIX = ".jpg";

    private static final String DARK_MODE_SUFFIX = "_dark";

    private static final float DARK_MODE_BRIGHTNESS = 0.45f;

    private static final float OPAQUE = 1f;

    private static final int MAX_EDGE_PX = 2560;

    private static final int JPEG_QUALITY = 90;

    private static final Set<File> UNAPPLIED_FILES = new HashSet<>();

    private PictureStore() {
    }

    static File file(Context context, String name) {
        return new File(directory(context), name);
    }

    static File darkModeFile(File picture) {
        File dimmed = dimmedFile(picture);
        return dimmed.isFile() ? dimmed : picture;
    }

    static void deleteOtherPictures(SavedPicture kept) {
        File[] files = kept.file.getParentFile().listFiles();
        if (files == null) {
            return;
        }
        synchronized (UNAPPLIED_FILES) {
            for (File file : files) {
                if (file.equals(kept.file) || file.equals(kept.darkModeFile) || UNAPPLIED_FILES.contains(file)) {
                    continue;
                }
                if (!file.delete()) {
                    Log.w(PatchSettings.LOG_TAG, "Cannot delete the old custom theme picture " + file.getName());
                }
            }
        }
    }

    static void markApplied(SavedPicture saved) {
        releaseFiles(saved.file, dimmedFile(saved.file));
    }

    static SavedPicture save(Context context, Uri source) throws IOException {
        Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), source),
                (decoder, info, ignored) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int width = info.getSize().getWidth();
                    int height = info.getSize().getHeight();
                    int longestEdge = Math.max(width, height);
                    if (longestEdge > MAX_EDGE_PX) {
                        decoder.setTargetSize(scaledEdge(width, longestEdge), scaledEdge(height, longestEdge));
                    }
                });
        try {
            File directory = directory(context);
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Cannot create " + directory.getName());
            }
            File target = new File(directory, FILE_PREFIX + System.currentTimeMillis() + FILE_SUFFIX);
            File dimmed = dimmedFile(target);
            synchronized (UNAPPLIED_FILES) {
                UNAPPLIED_FILES.add(target);
                UNAPPLIED_FILES.add(dimmed);
            }
            try {
                writeJpeg(bitmap, target);
                int averageColor = averageColor(bitmap);
                if (ThemeColorRamp.hasWhiteTextContrast(averageColor)) {
                    return new SavedPicture(target, target, averageColor);
                }
                writeDimmedJpeg(bitmap, dimmed);
                return new SavedPicture(target, dimmed, averageColor);
            } catch (IOException | RuntimeException ex) {
                deleteUnsavedFile(target);
                deleteUnsavedFile(dimmed);
                releaseFiles(target, dimmed);
                throw ex;
            }
        } finally {
            bitmap.recycle();
        }
    }

    private static void deleteUnsavedFile(File file) {
        if (file.exists() && !file.delete()) {
            Log.w(PatchSettings.LOG_TAG, "Cannot delete the unsaved custom theme picture " + file.getName());
        }
    }

    private static void writeDimmedJpeg(Bitmap bitmap, File target) throws IOException {
        Bitmap dimmed = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        try {
            ColorMatrix scale = new ColorMatrix();
            scale.setScale(DARK_MODE_BRIGHTNESS, DARK_MODE_BRIGHTNESS, DARK_MODE_BRIGHTNESS, OPAQUE);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(scale));
            new Canvas(dimmed).drawBitmap(bitmap, 0, 0, paint);
            writeJpeg(dimmed, target);
        } finally {
            dimmed.recycle();
        }
    }

    private static void writeJpeg(Bitmap bitmap, File target) throws IOException {
        try (OutputStream output = new FileOutputStream(target)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                throw new IOException("Cannot encode " + target.getName());
            }
        }
    }

    private static void releaseFiles(File picture, File dimmed) {
        synchronized (UNAPPLIED_FILES) {
            UNAPPLIED_FILES.remove(picture);
            UNAPPLIED_FILES.remove(dimmed);
        }
    }

    private static File dimmedFile(File picture) {
        String name = picture.getName();
        String base = name.endsWith(FILE_SUFFIX) ? name.substring(0, name.length() - FILE_SUFFIX.length()) : name;
        return new File(picture.getParentFile(), base + DARK_MODE_SUFFIX + FILE_SUFFIX);
    }

    private static File directory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY);
    }

    private static int scaledEdge(int edge, int longestEdge) {
        return (int) Math.max(1, (long) edge * MAX_EDGE_PX / longestEdge);
    }

    private static int averageColor(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] row = new int[width];
        long red = 0;
        long green = 0;
        long blue = 0;
        for (int y = 0; y < height; y++) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1);
            for (int pixel : row) {
                red += Color.red(pixel);
                green += Color.green(pixel);
                blue += Color.blue(pixel);
            }
        }
        long pixels = (long) width * height;
        return Color.rgb((int) (red / pixels), (int) (green / pixels), (int) (blue / pixels));
    }

}

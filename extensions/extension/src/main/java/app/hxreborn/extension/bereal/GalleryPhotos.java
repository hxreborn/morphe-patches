/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class GalleryPhotos {

    private static final String TAG = "GalleryPhotos";

    private static final String DIRECTORY = "hxreborn-gallery";

    private static final int MIN_DECODED_SIDE = 1440;

    private static final boolean[] CAPTURED_SIDES = new boolean[2];

    private GalleryPhotos() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static void cameraOpened(Object cameraViewModel, Object hostContext, Object lifecycleOwner) {
        if (!PatchSettings.GALLERY_PHOTOS.get()) {
            return;
        }
        GalleryButton.show(PatchContext.activityFor(hostContext), lifecycleOwner, cameraViewModel);
    }

    public static Bitmap replace(boolean front) {
        if (!PatchSettings.GALLERY_PHOTOS.get()) {
            return null;
        }
        if (GalleryButton.hasPendingWrites()) {
            return null;
        }
        final Context context = Utils.getContext();
        final File photo = file(context, front);
        Bitmap decoded = null;
        if (photo.isFile()) {
            try {
                decoded = decode(photo);
                if (decoded == null) {
                    Log.e(TAG, "Gallery photo could not be decoded: " + photo);
                }
            } catch (OutOfMemoryError | RuntimeException ex) {
                Log.e(TAG, "Gallery photo not decoded: " + photo, ex);
            }
        }
        synchronized (CAPTURED_SIDES) {
            CAPTURED_SIDES[front ? 1 : 0] = true;
            if (CAPTURED_SIDES[0] && CAPTURED_SIDES[1]) {
                clear(context);
            }
        }
        return decoded;
    }

    static File file(Context context, boolean front) {
        return new File(directory(context), front ? "front.jpg" : "back.jpg");
    }

    static File directory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY);
    }

    static void store(Context context, Uri source, File target) throws IOException {
        final File directory = target.getParentFile();
        if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create " + directory);
        }
        final File partial = new File(target.getPath() + ".part");
        try (InputStream input = context.getContentResolver().openInputStream(source);
                OutputStream output = new FileOutputStream(partial)) {
            if (input == null) {
                throw new IOException("Cannot open " + source);
            }
            final byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        if (!partial.renameTo(target)) {
            throw new IOException("Cannot move " + partial + " to " + target);
        }
    }

    static void clear(Context context) {
        synchronized (CAPTURED_SIDES) {
            CAPTURED_SIDES[0] = false;
            CAPTURED_SIDES[1] = false;
        }
        final File[] files = directory(context).listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    static Bitmap decode(File photo) {
        final BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photo.getPath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(Math.min(bounds.outWidth, bounds.outHeight));
        final Bitmap bitmap = BitmapFactory.decodeFile(photo.getPath(), options);
        return (bitmap != null) ? rotate(photo, bitmap) : null;
    }

    static Bitmap preview(File photo, int width) {
        if (!photo.isFile()) {
            return null;
        }
        final BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photo.getPath(), bounds);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, Math.min(bounds.outWidth, bounds.outHeight) / Math.max(1, width));
        try {
            final Bitmap bitmap = BitmapFactory.decodeFile(photo.getPath(), options);
            return (bitmap != null) ? rotate(photo, bitmap) : null;
        } catch (OutOfMemoryError | RuntimeException ex) {
            Log.e(TAG, "Preview not decoded: " + photo, ex);
            return null;
        }
    }

    private static Bitmap rotate(File photo, Bitmap bitmap) {
        final Matrix orientation = orientation(photo);
        if (orientation == null) {
            return bitmap;
        }
        final Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), orientation,
                true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    private static int sampleSize(int shortestSide) {
        int sampleSize = 1;
        while (shortestSide / (sampleSize * 2) >= MIN_DECODED_SIDE) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static Matrix orientation(File photo) {
        final int value;
        try {
            value = new ExifInterface(photo.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException | RuntimeException ex) {
            Log.e(TAG, "Photo orientation not read: " + photo, ex);
            return null;
        }
        final Matrix matrix = new Matrix();
        switch (value) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return null;
        }
        return matrix;
    }

}

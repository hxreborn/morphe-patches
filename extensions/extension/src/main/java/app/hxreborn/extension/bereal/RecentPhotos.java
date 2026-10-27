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
package app.hxreborn.extension.bereal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

final class RecentPhotos {

    private static final String TAG = "RecentPhotos";

    private static final String READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES";

    private static final String READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED";

    private RecentPhotos() {
    }

    static String[] permissions() {
        if (Build.VERSION.SDK_INT >= 34) {
            return new String[] { READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED };
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[] { READ_MEDIA_IMAGES };
        }
        return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
    }

    static boolean hasAccess(Context context) {
        for (String permission : permissions()) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    static List<Uri> recent(ContentResolver resolver, int limit) {
        final List<Uri> photos = new ArrayList<>();
        try (Cursor cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC, " + MediaStore.Images.Media._ID + " DESC")) {
            while (cursor != null && cursor.moveToNext() && photos.size() < limit) {
                photos.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0)));
            }
        } catch (SecurityException | IllegalArgumentException ex) {
            Log.e(TAG, "Recent photos not listed", ex);
            return new ArrayList<>();
        }
        return photos;
    }

    @SuppressWarnings("deprecation")
    static Bitmap thumbnail(ContentResolver resolver, Uri photo, int side) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            return resolver.loadThumbnail(photo, new Size(side, side), null);
        }
        return MediaStore.Images.Thumbnails.getThumbnail(resolver, ContentUris.parseId(photo),
                MediaStore.Images.Thumbnails.MINI_KIND, null);
    }

}

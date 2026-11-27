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
package app.hxreborn.extension.audiomack;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

final class CoverArt {

    private static final int JPEG_QUALITY = 90;

    private CoverArt() {
    }

    static byte[] readAsJpeg(File image) {
        if (image == null || !image.isFile()) {
            return null;
        }
        final Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
        if (bitmap == null) {
            Log.w(ExportActions.LOG_TAG, "Cover art file not decoded");
            return null;
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final boolean encoded = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        bitmap.recycle();
        if (!encoded) {
            Log.w(ExportActions.LOG_TAG, "Cover art not encoded as JPEG");
            return null;
        }
        return out.toByteArray();
    }

}

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

@SuppressWarnings("unused")
public final class ThemeArtwork {

    private static final int PNG_HEADER_BYTES = 24;

    private static final int PNG_SIGNATURE = 0x89504e47;

    private static final int PNG_WIDTH_OFFSET = 16;

    private static final int PNG_HEIGHT_OFFSET = 20;

    private static final int IMAGE_WIDTH = 1081;

    private static final int IMAGE_HEIGHT = 874;

    private static final int LOGO_LEFT = 842;

    private static final int LOGO_TOP = 209;

    private static final int LOGO_WIDTH = 195;

    private static final int LOGO_HEIGHT = 73;

    private static final int ALPHA_MASK = 0xff000000;

    private static final int LOGO_COLOR = 0x00ececee;

    private static final byte[] IMAGE_SHA256 = Base64.decode("kSyNByMFSyUxTrWQ8+z5DCVvrzor+lw8jMUcN18FxIE=",
            Base64.DEFAULT);

    private ThemeArtwork() {
    }

    public static Bitmap decodeHero(ByteBuffer encoded, long generator) {
        if (encoded == null || encoded.limit() < PNG_HEADER_BYTES) {
            return null;
        }
        ByteBuffer input = encoded.duplicate().order(ByteOrder.BIG_ENDIAN);
        input.rewind();
        if (input.getInt(0) != PNG_SIGNATURE || input.getInt(PNG_WIDTH_OFFSET) != IMAGE_WIDTH
                || input.getInt(PNG_HEIGHT_OFFSET) != IMAGE_HEIGHT) {
            return null;
        }
        byte[] bytes = new byte[input.remaining()];
        input.get(bytes);
        try {
            if (!MessageDigest.isEqual(IMAGE_SHA256, MessageDigest.getInstance("SHA-256").digest(bytes))) {
                return null;
            }
        } catch (NoSuchAlgorithmException ex) {
            Log.e(PatchSettings.LOG_TAG, "Could not identify product artwork", ex);
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null) {
            return null;
        }
        Bitmap themed = adjustHero(bitmap);
        onHeader(generator, themed.getWidth(), themed.getHeight());
        return themed;
    }

    private static Bitmap adjustHero(Bitmap bitmap) {
        Bitmap themed = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (themed == null) {
            return bitmap;
        }
        int[] pixels = new int[LOGO_WIDTH * LOGO_HEIGHT];
        themed.getPixels(pixels, 0, LOGO_WIDTH, LOGO_LEFT, LOGO_TOP, LOGO_WIDTH, LOGO_HEIGHT);
        for (int index = 0; index < pixels.length; index++) {
            pixels[index] = (pixels[index] & ALPHA_MASK) | LOGO_COLOR;
        }
        themed.setPixels(pixels, 0, LOGO_WIDTH, LOGO_LEFT, LOGO_TOP, LOGO_WIDTH, LOGO_HEIGHT);
        return themed;
    }

    private static void onHeader(long generator, int width, int height) {
        throw new IllegalStateException("The image header callback was not replaced");
    }

}

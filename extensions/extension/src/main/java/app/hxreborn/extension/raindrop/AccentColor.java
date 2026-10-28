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
package app.hxreborn.extension.raindrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.util.Log;

@SuppressWarnings("unused")
public final class AccentColor {

    private static final String TAG = "RaindropAccent";

    private static final String ASSET_SCHEME = "assets://";

    private static final String CACHE_FOLDER = "accent";

    private static final byte[] LIGHT_PLACEHOLDER = "#1988e0".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] DARK_PLACEHOLDER = "#DCC28F".getBytes(StandardCharsets.US_ASCII);

    private static final int FOOTER_HASH_LENGTH = 20;

    private AccentColor() {
    }

    public static String recoloredBundlePath(Context context, String assetUrl, boolean materialYou,
            String customColor) {
        String light;
        String dark;
        if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            light = hex(context.getColor(android.R.color.system_accent1_600));
            dark = hex(context.getColor(android.R.color.system_accent1_200));
        } else if (customColor != null) {
            light = customColor;
            dark = customColor;
        } else {
            return null;
        }
        if (!assetUrl.startsWith(ASSET_SCHEME)) {
            return null;
        }

        File folder = new File(context.getCodeCacheDir(), CACHE_FOLDER);
        File bundle = new File(folder, light.substring(1) + dark.substring(1) + ".bundle");
        if (bundle.isFile()) {
            return bundle.getPath();
        }
        try {
            byte[] bytes = readAsset(context, assetUrl.substring(ASSET_SCHEME.length()));
            if (!replaceOnce(bytes, LIGHT_PLACEHOLDER, light) || !replaceOnce(bytes, DARK_PLACEHOLDER, dark)) {
                Log.w(TAG, "accent placeholders not found exactly once");
                return null;
            }
            rehashFooter(bytes);
            replaceCachedBundle(folder, bundle, bytes);
            return bundle.getPath();
        } catch (IOException | NoSuchAlgorithmException ex) {
            Log.w(TAG, "accent not applied", ex);
            return null;
        }
    }

    static String hex(int color) {
        return String.format(Locale.ROOT, "#%06x", color & 0xFFFFFF);
    }

    static boolean replaceOnce(byte[] bytes, byte[] placeholder, String color) {
        int index = indexOf(bytes, placeholder, 0);
        if (index < 0 || indexOf(bytes, placeholder, index + 1) >= 0) {
            return false;
        }
        byte[] replacement = color.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(replacement, 0, bytes, index, placeholder.length);
        return true;
    }

    private static int indexOf(byte[] bytes, byte[] pattern, int from) {
        int last = bytes.length - pattern.length;
        for (int i = from; i <= last; i++) {
            int j = 0;
            while (j < pattern.length && bytes[i + j] == pattern[j]) {
                j++;
            }
            if (j == pattern.length) {
                return i;
            }
        }
        return -1;
    }

    private static void rehashFooter(byte[] bytes) throws NoSuchAlgorithmException {
        int hashed = bytes.length - FOOTER_HASH_LENGTH;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(bytes, 0, hashed);
        System.arraycopy(digest.digest(), 0, bytes, hashed, FOOTER_HASH_LENGTH);
    }

    private static byte[] readAsset(Context context, String name) throws IOException {
        try (InputStream input = context.getAssets().open(name)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1 << 16];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static void replaceCachedBundle(File folder, File bundle, byte[] bytes) throws IOException {
        File[] stale = folder.listFiles();
        if (stale != null) {
            for (File file : stale) {
                file.delete();
            }
        }
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("cannot create " + folder);
        }
        File partial = new File(folder, bundle.getName() + ".tmp");
        try (OutputStream output = new FileOutputStream(partial)) {
            output.write(bytes);
        }
        if (!partial.renameTo(bundle)) {
            throw new IOException("cannot rename " + partial);
        }
    }

}

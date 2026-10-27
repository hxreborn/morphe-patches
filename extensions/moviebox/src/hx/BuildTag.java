/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

final class BuildTag {
    static final String HEADER = "X-Client-Build";

    private static final String TAG = "hxreborn/moviebox";
    private static final String CERTIFICATE_ASSET = "hx.cer";
    private static final String SALT = "oneroom";
    private static final int RANDOM_BOUND = 1000;
    private static final long MICROS_PER_MILLI = 1000L;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final String certificateSha256;
    private final Random random = new Random();

    private BuildTag(String certificateSha256) {
        this.certificateSha256 = certificateSha256;
    }

    static BuildTag fromInstalledApp() {
        try (InputStream asset = InstalledApp.context().getAssets().open(CERTIFICATE_ASSET)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int read; (read = asset.read(buffer)) != -1;) {
                bytes.write(buffer, 0, read);
            }
            return new BuildTag(hex(digest("SHA-256", bytes.toByteArray())));
        } catch (Exception ex) {
            Log.e(TAG, "cannot read " + CERTIFICATE_ASSET, ex);
            return null;
        }
    }

    String next() {
        long now = System.currentTimeMillis();
        String timestamp = String.format(Locale.ROOT, "%d%06d%d", now / MILLIS_PER_SECOND,
                (now % MILLIS_PER_SECOND) * MICROS_PER_MILLI, this.random.nextInt(RANDOM_BOUND));
        String seed = this.certificateSha256 + SALT + timestamp;
        return timestamp + "." + hex(digest("MD5", seed.getBytes(StandardCharsets.UTF_8)));
    }

    static String addTo(String clientInfo, String tag) {
        try {
            return new JSONObject(clientInfo).put(HEADER, tag).toString();
        } catch (JSONException malformed) {
            Log.w(TAG, "client info not tagged: " + malformed.getMessage());
            return clientInfo;
        }
    }

    private static byte[] digest(String algorithm, byte[] input) {
        try {
            return MessageDigest.getInstance(algorithm).digest(input);
        } catch (NoSuchAlgorithmException unsupported) {
            throw new IllegalStateException(unsupported);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", value));
        }
        return out.toString();
    }
}

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
package hx.gateway;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;

import android.util.Log;
import hx.platform.InstalledApp;
import hx.util.Logs;
import hx.util.Streams;
import org.json.JSONException;
import org.json.JSONObject;

public final class BuildTag {

    static final String HEADER = "X-Client-Build";

    private static final String CERTIFICATE_ASSET = "hx.cer";

    private static final String SALT = "oneroom";

    private static final int RANDOM_BOUND = 1000;

    private static final long MICROS_PER_MILLI = 1000L;

    private static final long MILLIS_PER_SECOND = 1000L;

    private final String certificateSha256;

    private final MillisClock clock;

    private final Random random;

    private BuildTag(String certificateSha256, MillisClock clock, Random random) {
        this.certificateSha256 = certificateSha256;
        this.clock = clock;
        this.random = random;
    }

    public static BuildTag fromInstalledApp() {
        try (InputStream asset = InstalledApp.context().getAssets().open(CERTIFICATE_ASSET)) {
            return fromCertificate(Streams.readAll(asset));
        } catch (Exception ex) {
            Log.e(Logs.TAG, "cannot read " + CERTIFICATE_ASSET, ex);
            return null;
        }
    }

    static BuildTag fromCertificate(byte[] certificate) {
        return fromCertificate(certificate, System::currentTimeMillis, new Random());
    }

    static BuildTag fromCertificate(byte[] certificate, MillisClock clock, Random random) {
        try {
            return new BuildTag(Digests.sha256Hex(certificate), clock, random);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static String addTo(String clientInfo, String tag) {
        try {
            return new JSONObject(clientInfo).put(HEADER, tag).toString();
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "client info not tagged: " + ex.getMessage());
            return clientInfo;
        }
    }

    String next() {
        long nowMillis = this.clock.nowMillis();
        String timestamp = String.format(Locale.ROOT, "%d%06d%d", nowMillis / MILLIS_PER_SECOND,
                (nowMillis % MILLIS_PER_SECOND) * MICROS_PER_MILLI, this.random.nextInt(RANDOM_BOUND));
        String seed = this.certificateSha256 + SALT + timestamp;
        try {
            return timestamp + "." + Digests.md5Hex(seed.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

}

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

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

final class BuildTagInterceptor implements InvocationHandler {
    private static final String TAG = "hxreborn/moviebox";
    private static final String ACTIVITY_THREAD = "android.app.ActivityThread";
    private static final String CERTIFICATE_ASSET = "hx.cer";
    private static final String BUILD_HEADER = "X-Client-Build";
    private static final String INFO_HEADER = "X-Client-Info";
    private static final String SALT = "oneroom";
    private static final int RANDOM_BOUND = 1000;
    private static final long MICROS_PER_MILLI = 1000L;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final String certificateSha256;
    private final Random random = new Random();

    private BuildTagInterceptor(String certificateSha256) {
        this.certificateSha256 = certificateSha256;
    }

    static BuildTagInterceptor fromInstalledApp() {
        try {
            Context application = (Context) Class.forName(ACTIVITY_THREAD).getMethod("currentApplication")
                    .invoke(null);
            try (InputStream asset = application.getAssets().open(CERTIFICATE_ASSET)) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                for (int read; (read = asset.read(buffer)) != -1;) {
                    bytes.write(buffer, 0, read);
                }
                return new BuildTagInterceptor(hex(digest("SHA-256", bytes.toByteArray())));
            }
        } catch (Exception ex) {
            Log.e(TAG, "cannot read " + CERTIFICATE_ASSET, ex);
            return null;
        }
    }

    private String newTag() {
        long now = System.currentTimeMillis();
        String timestamp = String.format(Locale.ROOT, "%d%06d%d", now / MILLIS_PER_SECOND,
                (now % MILLIS_PER_SECOND) * MICROS_PER_MILLI, this.random.nextInt(RANDOM_BOUND));
        String seed = this.certificateSha256 + SALT + timestamp;
        return timestamp + "." + hex(digest("MD5", seed.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (args == null || args.length == 0) {
            if ("toString".equals(name)) return "MovieBoxBuildTagInterceptor";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        }
        if ("equals".equals(name) && args != null && args.length == 1) return proxy == args[0];
        if (args == null || args.length != 1) return null;

        try {
            return intercept(args[0]);
        } catch (InvocationTargetException wrapped) {
            throw asIoException(wrapped.getCause() != null ? wrapped.getCause() : wrapped);
        } catch (Throwable failure) {
            throw asIoException(failure);
        }
    }

    private static IOException asIoException(Throwable failure) {
        return failure instanceof IOException ? (IOException) failure : new IOException("MovieBox build tag failed", failure);
    }

    private Object intercept(Object chain) throws Throwable {
        Object request = chain.getClass().getMethod("request").invoke(chain);
        Object stamped = withStockCertificateTag(request);
        return chain.getClass().getMethod("proceed", request.getClass()).invoke(chain, stamped);
    }

    private Object withStockCertificateTag(Object request) throws ReflectiveOperationException {
        Method header = request.getClass().getMethod("header", String.class);
        if (header.invoke(request, BUILD_HEADER) == null) return request;

        String tag = newTag();
        Object builder = request.getClass().getMethod("newBuilder").invoke(request);
        Method set = builder.getClass().getMethod("header", String.class, String.class);
        set.invoke(builder, BUILD_HEADER, tag);
        String info = (String) header.invoke(request, INFO_HEADER);
        if (info != null) set.invoke(builder, INFO_HEADER, withTag(info, tag));
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private static String withTag(String clientInfo, String tag) {
        try {
            return new JSONObject(clientInfo).put(BUILD_HEADER, tag).toString();
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

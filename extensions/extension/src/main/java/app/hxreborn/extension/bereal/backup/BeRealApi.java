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
package app.hxreborn.extension.bereal.backup;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("unused")
public final class BeRealApi {

    private static final String TAG = "BeRealApi";

    private static final String MEMORIES_PATH = "feeds/memories-v1";

    private static final String MOMENT_PATH = "feeds/memories-v2";

    private static volatile HttpUrl baseUrl;

    private static volatile Call.Factory callFactory;

    // Media is public, so it goes out without the app's auth
    private static final OkHttpClient MEDIA_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    private BeRealApi() {
    }

    public static void onCreate(Object retrofit, Class<?> service) {
        try {
            if (!hasMemoriesEndpoint(service)) {
                return;
            }
            final Class<?> type = retrofit.getClass();
            final HttpUrl url = (HttpUrl) type.getMethod("baseUrl").invoke(retrofit);
            final Call.Factory factory = (Call.Factory) type.getMethod("callFactory").invoke(retrofit);
            baseUrl = url;
            callFactory = factory;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Could not capture Retrofit", ex);
        }
    }

    public static boolean isReady() {
        return baseUrl != null && callFactory != null;
    }

    public static String memories() throws IOException {
        return get(builder().addPathSegments(MEMORIES_PATH));
    }

    public static String moment(String momentId) throws IOException {
        if (!BackupRun.isValidMomentId(momentId)) {
            throw new IOException("Bad moment id");
        }
        return get(builder().addPathSegments(MOMENT_PATH).addPathSegment(momentId));
    }

    private static HttpUrl.Builder builder() throws IOException {
        final HttpUrl base = baseUrl;
        if (base == null) {
            throw new IOException("API not ready");
        }
        return base.newBuilder();
    }

    private static String get(HttpUrl.Builder builder) throws IOException {
        final HttpUrl url = builder.build();
        final HttpUrl base = baseUrl;
        // The authenticated client must only talk to the API host
        if (base == null || !base.host().equals(url.host())) {
            throw new IOException("Wrong host");
        }
        try (Response response = execute(url)) {
            return response.body().string();
        }
    }

    public static InputStream open(String absoluteUrl) throws IOException {
        final HttpUrl url = HttpUrl.parse(absoluteUrl);
        if (url == null || !url.isHttps()) {
            throw new IOException("Bad URL");
        }
        final Response response = MEDIA_CLIENT.newCall(new Request.Builder().url(url).build()).execute();
        if (!response.isSuccessful()) {
            final int code = response.code();
            response.close();
            throw new IOException("HTTP " + code);
        }
        return new FilterInputStream(response.body().byteStream()) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    response.close();
                }
            }
        };
    }

    private static Response execute(HttpUrl url) throws IOException {
        final Call.Factory factory = callFactory;
        if (factory == null) {
            throw new IOException("API not ready");
        }
        final Response response = factory.newCall(new Request.Builder().url(url).build()).execute();
        if (!response.isSuccessful()) {
            final int code = response.code();
            response.close();
            throw new IOException("HTTP " + code);
        }
        return response;
    }

    private static boolean hasMemoriesEndpoint(Class<?> service) {
        for (Method method : service.getDeclaredMethods()) {
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (annotation.toString().contains(MEMORIES_PATH)) {
                    return true;
                }
            }
        }
        return false;
    }

}

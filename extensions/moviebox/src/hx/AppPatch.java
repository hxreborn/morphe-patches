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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class AppPatch {
    private static final String TAG = "hxreborn/moviebox";

    private static final String MANAGER_CLASS = "fh.c";
    private static final String OKHTTP_CLIENT = "okhttp3.OkHttpClient";
    private static final String OKHTTP_INTERCEPTOR = "okhttp3.Interceptor";
    private static final String OKHTTP_URL = "okhttp3.HttpUrl";
    private static final String BFF = "wefeed-mobile-bff";
    private static final String PLAY_INFO = BFF + "/subject-api/play-info/v2";
    private static final String RESOURCE_POSITION = BFF + "/subject-api/resource-position/v2";
    private static final String PLAY_INFO_SEGMENT = "/subject-api/play-info/";
    private static final String RESOURCE_LIST_SEGMENT = "/subject-api/resource/";
    private static final String USER_PROFILE = BFF + "/user-api/profile";
    private static final String MEMBER_DETAIL = BFF + "/vip/member/detail";
    private static final String[] URL_FIELDS = {"url", "resourceLink", "downloadUrl", "playUrl"};
    private static final int MEMBER_DAYS_LEFT = 9999;
    private static final String MEMBER_EXPIRY = "2099-12-31";
    private static final String MMKV_CLASS = "com.tencent.mmkv.MMKV";
    private static final String MMKV_APP_ID = "kv_app";
    private static final String LABORATORY_PASSWORD_TIME = "lab_enter_password_time";
    private static final int MANAGER_RETRY_LIMIT = 200;
    private static final long MANAGER_RETRY_DELAY_MS = 50L;
    private static final String INSTALL_FAILED = "Playback patch failed to load";
    private static final String ACTIVITY_THREAD = "android.app.ActivityThread";
    private static final String NO_STREAM = "No signed stream for this title";
    private static final String NOT_HOSTED = "This title is not hosted";

    private static final Set<String> reported = Collections.synchronizedSet(new HashSet<>());

    private AppPatch() {
    }

    public static void install(ClassLoader preferred) {
        new Installer(preferred).start();
    }

    private static final class Installer implements Runnable {
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ClassLoader preferred;
        private int attempts;
        private boolean laboratoryUnlocked;
        private Throwable failure;

        Installer(ClassLoader preferred) {
            this.preferred = preferred;
        }

        void start() {
            main.post(this);
        }

        @Override
        public void run() {
            if (tryInstall()) return;
            if (++attempts < MANAGER_RETRY_LIMIT) {
                main.postDelayed(this, MANAGER_RETRY_DELAY_MS);
                return;
            }
            reportFailure();
        }

        private boolean tryInstall() {
            Class<?> manager = resolve(MANAGER_CLASS, preferred);
            if (manager == null) return false;
            if (!laboratoryUnlocked) {
                unlockLaboratory(manager.getClassLoader());
                laboratoryUnlocked = true;
            }
            try {
                doInstall(manager);
                return true;
            } catch (Throwable t) {
                failure = t;
                return false;
            }
        }

        private void reportFailure() {
            if (failure == null) {
                Log.e(TAG, "network manager class not found");
            } else {
                Log.e(TAG, "cannot install DASH interceptor", failure);
            }
            notice(INSTALL_FAILED);
        }
    }

    private static void diagnose(String message) {
        if (reported.add(message)) notice(message);
    }

    private static void notice(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Object application = Class.forName(ACTIVITY_THREAD).getMethod("currentApplication").invoke(null);
                    Toast.makeText((Context) application, message, Toast.LENGTH_LONG).show();
                } catch (Throwable unavailable) {
                    Log.e(TAG, "cannot show \"" + message + "\"", unavailable);
                }
            }
        });
    }

    private static Class<?> resolve(String name, ClassLoader preferred) {
        ClassLoader[] loaders = {
            preferred,
            AppPatch.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader(),
        };
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException absent) {
            }
        }
        return null;
    }

    private static void doInstall(Class<?> manager) throws Exception {
        Object instance = singleton(manager);
        if (instance == null) throw new IllegalStateException("no network manager singleton");

        ClassLoader loader = manager.getClassLoader();
        Class<?> clientClass = Class.forName(OKHTTP_CLIENT, false, loader);
        Field clientField = clientHolder(manager, clientClass);
        if (clientField == null) throw new IllegalStateException("no client field on network manager");
        Object client = clientField.get(instance);
        if (!clientClass.isInstance(client)) {
            throw new IllegalStateException("network manager field is not an OkHttpClient");
        }

        Field retrofitField = fieldInPackage(manager, "retrofit2.");
        Object retrofit = retrofitField == null ? null : retrofitField.get(instance);
        if (retrofit == null) throw new IllegalStateException("no Retrofit instance on network manager");
        Field callFactory = clientHolder(retrofit.getClass(), clientClass);
        if (callFactory == null) throw new IllegalStateException("no call factory field on Retrofit");
        Field baseUrl = fieldInPackage(retrofit.getClass(), OKHTTP_URL);
        if (baseUrl == null) throw new IllegalStateException("no base URL field on Retrofit");
        String apiBase = String.valueOf(baseUrl.get(retrofit));

        MovieBoxSource source = new MovieBoxSource(loader, client, apiBase);
        DashServer server = null;
        if (isMainProcess()) {
            try {
                server = DashServer.bind(source);
            } catch (IOException e) {
                Log.e(TAG, "loopback server unavailable, resource lists keep their own links", e);
            }
        }
        boolean routeDownloads = !isMainProcess() || server != null;
        try {
            if (server != null) server.start();
            Object wrapped = addInterceptor(client, interceptor(loader, source, routeDownloads));
            clientField.set(instance, wrapped);
            try {
                callFactory.set(retrofit, wrapped);
            } catch (Exception e) {
                clientField.set(instance, client);
                throw e;
            }
        } catch (Exception e) {
            if (server != null) server.close();
            throw e;
        }
        Log.i(TAG, "DASH interceptor installed");
    }

    private static void unlockLaboratory(ClassLoader loader) {
        try {
            Class<?> mmkvClass = Class.forName(MMKV_CLASS, false, loader);
            Method mmkvWithId = null;
            for (Method candidate : mmkvClass.getDeclaredMethods()) {
                Class<?>[] parameters = candidate.getParameterTypes();
                if (Modifier.isStatic(candidate.getModifiers())
                        && candidate.getReturnType() == mmkvClass
                        && parameters.length == 1
                        && parameters[0] == String.class) {
                    mmkvWithId = candidate;
                    break;
                }
            }
            if (mmkvWithId == null) {
                throw new IllegalStateException("no mmkvWithID(String) on " + MMKV_CLASS);
            }

            SharedPreferences.Editor store =
                    (SharedPreferences.Editor) mmkvWithId.invoke(null, MMKV_APP_ID);
            store.putLong(LABORATORY_PASSWORD_TIME, Long.MAX_VALUE);
            Log.i(TAG, "Laboratory unlocked");
        } catch (Exception e) {
            Log.e(TAG, "cannot unlock Laboratory", e);
        }
    }

    private static boolean isMainProcess() {
        String process = Application.getProcessName();
        return process != null && !process.contains(":");
    }

    private static Object singleton(Class<?> manager) throws Exception {
        for (Field field : manager.getDeclaredFields()) {
            if ("kotlin.Lazy".equals(field.getType().getName())) {
                field.setAccessible(true);
                Object lazy = field.get(null);
                if (lazy == null) return null;
                return lazy.getClass().getMethod("getValue").invoke(lazy);
            }
        }
        return null;
    }

    private static Field clientHolder(Class<?> owner, Class<?> clientClass) {
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != Object.class && field.getType().isAssignableFrom(clientClass)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }

    private static Field fieldInPackage(Class<?> owner, String prefix) {
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType().getName().startsWith(prefix)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }

    private static Object addInterceptor(Object client, Object proxy) throws Exception {
        Class<?> clientClass = client.getClass();
        Object builder = clientClass.getMethod("newBuilder").invoke(client);
        Object interceptors = builder.getClass().getMethod("interceptors").invoke(builder);
        interceptors.getClass().getMethod("add", int.class, Object.class).invoke(interceptors, 0, proxy);
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private static Object interceptor(ClassLoader loader, MovieBoxSource source, boolean routeDownloads)
            throws Exception {
        Class<?> interceptorClass = Class.forName(OKHTTP_INTERCEPTOR, false, loader);
        return Proxy.newProxyInstance(loader, new Class<?>[]{interceptorClass},
                new DashInterceptor(loader, source, routeDownloads));
    }

    private static final class SignedResource {
        private static final String POLICY_KEY = "CloudFront-Policy=";
        private static final String EDGE_KEY = "Edge-Cache-Cookie=";

        final String cookie;
        final String manifestUrl;

        private SignedResource(String cookie, String manifestUrl) {
            this.cookie = cookie;
            this.manifestUrl = manifestUrl;
        }

        boolean isEdgeCache() {
            return cookie.contains(EDGE_KEY);
        }

        static SignedResource fromCookie(String cookie) {
            SignedResource cloudFront = fromCloudFront(cookie);
            return cloudFront != null ? cloudFront : fromEdgeCache(cookie);
        }

        private static SignedResource fromCloudFront(String cookie) {
            String policy = extractFieldValue(cookie, POLICY_KEY, ';');
            if (policy == null) return null;
            try {
                String standard = policy.replace('-', '+').replace('_', '=').replace('~', '/');
                String json = new String(Base64.decode(standard, Base64.DEFAULT), StandardCharsets.UTF_8);
                String resource = new JSONObject(json).getJSONArray("Statement").getJSONObject(0).getString("Resource");
                if (!resource.startsWith("https://") || !resource.endsWith("/*")) return null;
                return new SignedResource(cookie, resource.substring(0, resource.length() - 2) + "/index.mpd");
            } catch (JSONException | IllegalArgumentException malformed) {
                return null;
            }
        }

        private static SignedResource fromEdgeCache(String cookie) {
            String value = extractFieldValue(cookie, EDGE_KEY, ';');
            if (value == null) return null;
            String prefixEncoded = extractFieldValue(value, "urlprefix=", ':');
            if (prefixEncoded == null) return null;
            try {
                String urlPrefix = new String(Base64.decode(prefixEncoded, Base64.URL_SAFE), StandardCharsets.UTF_8);
                if (!urlPrefix.startsWith("https://")) return null;
                if (!urlPrefix.endsWith("/")) urlPrefix = urlPrefix + "/";
                return new SignedResource(cookie, urlPrefix + "index.mpd");
            } catch (IllegalArgumentException malformed) {
                return null;
            }
        }

        static SignedResource fromPlayInfo(String body) {
            try {
                JSONArray streams = new JSONObject(body).getJSONObject("data").getJSONArray("streams");
                for (int i = 0; i < streams.length(); i++) {
                    SignedResource resource = fromCookie(streams.getJSONObject(i).optString("signCookie", ""));
                    if (resource != null) return resource;
                }
            } catch (JSONException malformed) {
            }
            return null;
        }

        static String key(String subjectId, int season, int episode) {
            return subjectId + "/" + season + "/" + episode;
        }

        static String key(Uri playInfoRequest) {
            String subjectId = playInfoRequest.getQueryParameter("subjectId");
            String season = playInfoRequest.getQueryParameter("se");
            String episode = playInfoRequest.getQueryParameter("ep");
            if (subjectId == null || season == null || episode == null) return null;
            try {
                return key(subjectId, Integer.parseInt(season), Integer.parseInt(episode));
            } catch (NumberFormatException malformed) {
                return null;
            }
        }

        private static String extractFieldValue(String source, String key, char delimiter) {
            int at = source.indexOf(key);
            if (at < 0) return null;
            int start = at + key.length();
            int end = source.indexOf(delimiter, start);
            return end < 0 ? source.substring(start) : source.substring(start, end);
        }
    }

    private static final class NoSignedResourceException extends IOException {
        private static final long serialVersionUID = 1L;

        NoSignedResourceException(String message) {
            super(message);
        }
    }

    private static final class MovieBoxSource implements DashServer.Source {
        private static final int FILE_RETENTION = 8;
        private static final int RESOURCE_RETENTION = 32;
        private static final int ORIGIN_PROBE_TIMEOUT_MS = 10000;
        private static final long PLACEHOLDER_SIZE_RATIO = 2;
        private static final int PLAY_INFO_ATTEMPTS = 5;
        private static final long PLAY_INFO_RETRY_DELAY_MS = 2000L;

        private final ClassLoader loader;
        private final Object client;
        private final String apiBase;
        private final Map<String, SignedResource> resources = newLruMap(RESOURCE_RETENTION);
        private final Map<String, Long> originLengths = newLruMap(RESOURCE_RETENTION);
        private final Map<String, Boolean> directFiles = newLruMap(RESOURCE_RETENTION);
        private final Map<String, Boolean> handedOff = newLruMap(RESOURCE_RETENTION);
        private final Map<String, FutureTask<DashFile>> files = newLruMap(FILE_RETENTION);
        private final CloudFrontPlayInfoClient cloudFrontPlayInfoClient = new CloudFrontPlayInfoClient();

        MovieBoxSource(ClassLoader loader, Object client, String apiBase) {
            this.loader = loader;
            this.client = client;
            this.apiBase = apiBase.endsWith("/") ? apiBase : apiBase + "/";
        }

        SignedResource observePlayInfo(String requestUrl, String body, String apiHost) {
            String key = SignedResource.key(Uri.parse(requestUrl));
            SignedResource resource = SignedResource.fromPlayInfo(body);
            if (resource == null) {
                Log.w(TAG, "play-info " + key + ": no signed DASH resource");
                diagnose(NO_STREAM);
                return null;
            }
            Log.i(TAG, "play-info " + key + ": DASH on " + Uri.parse(resource.manifestUrl).getHost()
                    + (apiHost == null ? "" : ", app API host " + apiHost));
            if (key == null) return null;
            synchronized (resources) {
                resources.put(key, resource);
            }
            return resource;
        }

        @Override
        public DashFile open(final String subjectId, final int season, final int episode, final int height,
                             String origin, long originSize, String resourceId, boolean fromStart) throws IOException {
            String label = SignedResource.key(subjectId, season, episode) + "@" + height;
            if (origin != null && originSize > 0 && originLength(origin) == originSize) {
                Log.i(TAG, label + ": redirect to origin, size " + originSize + " matches");
                return null;
            }
            SignedResource signed;
            try {
                signed = resource(subjectId, season, episode, null);
            } catch (NoSignedResourceException noResource) {
                if (origin == null) throw noResource;
                if (isPlaceholder(origin, originSize)) {
                    diagnose(NOT_HOSTED);
                    throw new DashServer.UnavailableException(label + ": origin is a placeholder for " + originSize + " bytes");
                }
                Log.i(TAG, label + ": redirect to origin, " + noResource.getMessage());
                return null;
            }
            if (isHandedOff(label, subjectId, signed, resourceId, origin, originSize, fromStart)) {
                throw new DashServer.UnavailableException(label + ": DASH on " + Uri.parse(signed.manifestUrl).getHost()
                        + ", leaving the download to the app's direct file");
            }
            return getOrCreateFile(subjectId, season, episode, height);
        }

        private boolean isHandedOff(String label, String subjectId, SignedResource signed, String resourceId,
                                    String origin, long originSize, boolean fromStart) {
            synchronized (handedOff) {
                Boolean decided = handedOff.get(label);
                if (decided != null) return decided;
            }
            boolean handOff = fromStart && !DashFile.isUnpacedCdn(signed.manifestUrl)
                    && hasDirectFile(subjectId, resourceId, origin, originSize);
            synchronized (handedOff) {
                Boolean decided = handedOff.get(label);
                if (decided != null) return decided;
                handedOff.put(label, handOff);
                return handOff;
            }
        }

        private boolean hasDirectFile(String subjectId, String resourceId, String origin, long originSize) {
            if (resourceId == null || origin == null || originSize <= 0) return false;
            synchronized (directFiles) {
                Boolean known = directFiles.get(resourceId);
                if (known != null) return known;
            }
            try {
                String body = get(apiBase + RESOURCE_POSITION + "?subjectId=" + subjectId + "&resourceId=" + resourceId
                        + "&failUrl=" + Uri.encode(origin) + "&failCode=404&resourceNum=0&isVip=true");
                JSONArray list = new JSONObject(body).getJSONObject("data").optJSONArray("list");
                String url = list == null || list.length() == 0 ? "" : list.getJSONObject(0).optString("resourceLink", "");
                long length = url.isEmpty() ? -1 : probeLength(url);
                boolean available = length == originSize;
                Log.i(TAG, "direct file " + resourceId + ": " + (url.isEmpty() ? "none"
                        : Uri.parse(url).getHost() + ", " + length + " of " + originSize + " bytes"));
                synchronized (directFiles) {
                    directFiles.put(resourceId, available);
                }
                return available;
            } catch (IOException | JSONException | NumberFormatException e) {
                Log.w(TAG, "direct file " + resourceId + " not checked", e);
                return false;
            }
        }

        private boolean isPlaceholder(String origin, long originSize) {
            long actual = originLength(origin);
            return originSize > 0 && actual > 0 && actual * PLACEHOLDER_SIZE_RATIO < originSize;
        }

        private long originLength(String origin) {
            synchronized (originLengths) {
                Long known = originLengths.get(origin);
                if (known != null) return known;
            }
            long length;
            try {
                length = probeLength(origin);
            } catch (IOException | NumberFormatException unusable) {
                Log.w(TAG, "origin " + origin + " not probed", unusable);
                return -1;
            }
            Log.i(TAG, "origin " + origin + ": " + length + " bytes");
            synchronized (originLengths) {
                originLengths.put(origin, length);
            }
            return length;
        }

        private static long probeLength(String origin) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(origin).openConnection();
            connection.setConnectTimeout(ORIGIN_PROBE_TIMEOUT_MS);
            connection.setReadTimeout(ORIGIN_PROBE_TIMEOUT_MS);
            connection.setRequestProperty("Range", "bytes=0-0");
            try {
                int code = connection.getResponseCode();
                String contentRange = connection.getHeaderField("Content-Range");
                int slash = contentRange == null ? -1 : contentRange.indexOf('/');
                if (code == HttpURLConnection.HTTP_PARTIAL && slash >= 0) {
                    return Long.parseLong(contentRange.substring(slash + 1).trim());
                }
                if (code == HttpURLConnection.HTTP_OK) return connection.getContentLengthLong();
                throw new IOException("HTTP " + code);
            } finally {
                connection.disconnect();
            }
        }

        private DashFile getOrCreateFile(final String subjectId, final int season, final int episode, final int height)
                throws IOException {
            final String key = SignedResource.key(subjectId, season, episode) + "/" + height;
            FutureTask<DashFile> task;
            boolean owner = false;
            synchronized (files) {
                task = files.get(key);
                if (task == null) {
                    task = new FutureTask<>(new Callable<DashFile>() {
                        @Override
                        public DashFile call() throws IOException {
                            SignedResource signed = resource(subjectId, season, episode, null);
                            long started = SystemClock.elapsedRealtime();
                            DashFile file = DashFile.open(signed.manifestUrl, height,
                                    new PinnedCookies(signed, subjectId, season, episode));
                            Log.i(TAG, "DASH file " + key + ": " + file.length + " bytes from "
                                    + Uri.parse(signed.manifestUrl).getHost() + ", ready in "
                                    + (SystemClock.elapsedRealtime() - started) + " ms");
                            return file;
                        }
                    });
                    files.put(key, task);
                    owner = true;
                }
            }
            if (owner) task.run();
            try {
                return task.get();
            } catch (ExecutionException e) {
                synchronized (files) {
                    if (files.get(key) == task) files.remove(key);
                }
                Throwable cause = e.getCause();
                throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }

        private final class PinnedCookies implements DashFile.CookieProvider {
            private final String manifestUrl;
            private final String subjectId;
            private final int season;
            private final int episode;
            private String cookie;

            PinnedCookies(SignedResource signed, String subjectId, int season, int episode) {
                this.manifestUrl = signed.manifestUrl;
                this.cookie = signed.cookie;
                this.subjectId = subjectId;
                this.season = season;
                this.episode = episode;
            }

            @Override
            public synchronized String current() {
                return cookie;
            }

            @Override
            public synchronized String refresh(String rejected) throws IOException {
                if (!cookie.equals(rejected)) return cookie;
                SignedResource fresh = resource(subjectId, season, episode, rejected);
                if (!fresh.manifestUrl.equals(manifestUrl)) {
                    throw new IOException("signed DASH resource moved to " + Uri.parse(fresh.manifestUrl).getHost());
                }
                cookie = fresh.cookie;
                return cookie;
            }
        }

        private SignedResource resource(String subjectId, int season, int episode, String rejectedCookie)
                throws IOException {
            String key = SignedResource.key(subjectId, season, episode);
            SignedResource cached;
            synchronized (resources) {
                cached = resources.get(key);
            }
            if (cached != null && !cached.cookie.equals(rejectedCookie)) return cached;
            String url = apiBase + PLAY_INFO + "?subjectId=" + subjectId + "&se=" + season + "&ep=" + episode
                    + "&isVip=true";
            String body = getPlayInfoWithRetry(url, key);
            if (isEdgeCachePlayInfo(body)) {
                String replacement = fetchReplacementPlayInfo("/" + PLAY_INFO + "?subjectId=" + subjectId
                        + "&se=" + season + "&ep=" + episode + "&isVip=true");
                if (replacement != null) body = replacement;
            }
            SignedResource fetched = observePlayInfo(url, body, null);
            if (fetched == null) throw new NoSignedResourceException("play-info has no signed DASH resource for " + key);
            return fetched;
        }

        static boolean isEdgeCachePlayInfo(String playInfo) {
            SignedResource resource = SignedResource.fromPlayInfo(playInfo);
            return resource != null && resource.isEdgeCache();
        }

        String fetchReplacementPlayInfo(String pathAndQuery) {
            return cloudFrontPlayInfoClient.fetchPlayInfo(pathAndQuery, body -> {
                SignedResource resource = SignedResource.fromPlayInfo(body);
                if (resource != null && DashFile.isUnpacedCdn(resource.manifestUrl)) return true;
                Log.i(TAG, "alternative play-info " + (resource == null
                        ? "has no signed DASH resource" : "is on " + Uri.parse(resource.manifestUrl).getHost()));
                return false;
            });
        }

        private String getPlayInfoWithRetry(String url, String key) throws IOException {
            for (int attempt = 1; ; attempt++) {
                try {
                    return get(url);
                } catch (IOException e) {
                    if (attempt == PLAY_INFO_ATTEMPTS) throw e;
                    Log.w(TAG, "play-info " + key + " attempt " + attempt + " of " + PLAY_INFO_ATTEMPTS + " failed", e);
                    SystemClock.sleep(PLAY_INFO_RETRY_DELAY_MS);
                }
            }
        }

        private String get(String url) throws IOException {
            try {
                Object builder = Class.forName("okhttp3.Request$Builder", false, loader).getConstructor().newInstance();
                builder.getClass().getMethod("url", String.class).invoke(builder, url);
                Object request = builder.getClass().getMethod("build").invoke(builder);
                Object call = client.getClass().getMethod("newCall", request.getClass()).invoke(client, request);
                Object response = call.getClass().getMethod("execute").invoke(call);
                try {
                    int code = (Integer) response.getClass().getMethod("code").invoke(response);
                    if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " for " + url);
                    Object body = response.getClass().getMethod("body").invoke(response);
                    return (String) body.getClass().getMethod("string").invoke(body);
                } finally {
                    response.getClass().getMethod("close").invoke(response);
                }
            } catch (InvocationTargetException e) {
                throw new IOException(e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new IOException(e);
            }
        }

        private static <V> Map<String, V> newLruMap(final int capacity) {
            return new LinkedHashMap<String, V>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                    return size() > capacity;
                }
            };
        }
    }

    private static final class DashInterceptor implements InvocationHandler {
        private final ClassLoader loader;
        private final MovieBoxSource source;
        private final boolean routeDownloads;

        DashInterceptor(ClassLoader loader, MovieBoxSource source, boolean routeDownloads) {
            this.loader = loader;
            this.source = source;
            this.routeDownloads = routeDownloads;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (args == null || args.length == 0) {
                if ("toString".equals(name)) return "MovieBoxDashInterceptor";
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
            return failure instanceof IOException ? (IOException) failure : new IOException("MovieBox interceptor failed", failure);
        }

        private Object intercept(Object chain) throws Throwable {
            Object request = chain.getClass().getMethod("request").invoke(chain);
            Object response = chain.getClass().getMethod("proceed", request.getClass()).invoke(chain, request);

            Object httpUrl = request.getClass().getMethod("url").invoke(request);
            String url = String.valueOf(httpUrl);
            boolean playInfo = url.contains(PLAY_INFO_SEGMENT);
            boolean resourceList = url.contains(RESOURCE_LIST_SEGMENT);
            boolean memberInfo = url.contains(USER_PROFILE) || url.contains(MEMBER_DETAIL);
            if (!playInfo && !resourceList && !memberInfo) return response;

            Object body = response.getClass().getMethod("body").invoke(response);
            if (body == null) return response;

            byte[] bytes = (byte[]) body.getClass().getMethod("bytes").invoke(body);
            byte[] payload;
            try {
                payload = rewriteBody(url, response, bytes, playInfo, resourceList);
            } catch (Throwable failure) {
                Log.e(TAG, Uri.parse(url).getPath() + " not rewritten", failure);
                payload = bytes;
            }
            return withBody(response, body, payload);
        }

        private byte[] rewriteBody(String url, Object response, byte[] bytes, boolean playInfo, boolean resourceList)
                throws ReflectiveOperationException {
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (playInfo && MovieBoxSource.isEdgeCachePlayInfo(text)) {
                Uri requested = Uri.parse(url);
                String replacement = source.fetchReplacementPlayInfo(
                        requested.getEncodedPath() + "?" + requested.getEncodedQuery());
                if (replacement != null) {
                    text = replacement;
                    bytes = replacement.getBytes(StandardCharsets.UTF_8);
                }
            }
            String out;
            if (playInfo) {
                Object sent = response.getClass().getMethod("request").invoke(response);
                source.observePlayInfo(url, text, Uri.parse(String.valueOf(
                        sent.getClass().getMethod("url").invoke(sent))).getHost());
                Uri playInfoUri = Uri.parse(url);
                out = rewritePlayInfo(text, playInfoUri.getQueryParameter("subjectId"),
                        parseQueryInt(playInfoUri, "se"), parseQueryInt(playInfoUri, "ep"));
            } else if (resourceList) {
                out = routeDownloads ? rewriteResourceList(text) : text;
            } else {
                out = rewriteMemberDays(text);
            }
            return out.equals(text) ? bytes : out.getBytes(StandardCharsets.UTF_8);
        }

        private Object withBody(Object response, Object body, byte[] payload) throws Throwable {
            Object mediaType = body.getClass().getMethod("contentType").invoke(body);
            Object newBody = createResponseBody(mediaType, payload);

            Object builder = response.getClass().getMethod("newBuilder").invoke(response);
            Class<?> bodyClass = Class.forName("okhttp3.ResponseBody", false, loader);
            builder.getClass().getMethod("body", bodyClass).invoke(builder, newBody);
            builder.getClass().getMethod("removeHeader", String.class).invoke(builder, "Content-Length");
            return builder.getClass().getMethod("build").invoke(builder);
        }

        private Object createResponseBody(Object mediaType, byte[] bytes) throws Throwable {
            Class<?> bodyClass = Class.forName("okhttp3.ResponseBody", false, loader);
            Class<?> mediaTypeClass = Class.forName("okhttp3.MediaType", false, loader);
            for (Method create : bodyClass.getMethods()) {
                if (!create.getName().equals("create")) continue;
                Class<?>[] parameters = create.getParameterTypes();
                if (parameters.length != 2) continue;
                if (parameters[0] == mediaTypeClass && parameters[1] == byte[].class) {
                    return create.invoke(null, mediaType, bytes);
                }
                if (parameters[0] == byte[].class && parameters[1] == mediaTypeClass) {
                    return create.invoke(null, bytes, mediaType);
                }
            }
            throw new IllegalStateException("no ResponseBody.create(byte[], MediaType) overload");
        }
    }

    static String rewritePlayInfo(String body, String subjectId, int season, int episode) {
        try {
            JSONObject root = new JSONObject(body);
            boolean changed = rewritePlaybackUrls(root);
            changed |= routeProgressiveStreams(root, subjectId, season, episode);
            return changed ? root.toString() : body;
        } catch (JSONException malformed) {
            Log.w(TAG, "play-info not rewritten: " + malformed.getMessage());
            return body;
        }
    }

    private static boolean routeProgressiveStreams(JSONObject root, String subjectId, int season, int episode)
            throws JSONException {
        if (subjectId == null || season < 0 || episode < 0) return false;
        JSONObject data = root.optJSONObject("data");
        JSONArray streams = data == null ? null : data.optJSONArray("streams");
        if (streams == null) return false;
        boolean changed = false;
        for (int i = 0; i < streams.length(); i++) {
            JSONObject stream = streams.optJSONObject(i);
            if (stream == null || !stream.optString("signCookie", "").isEmpty()) continue;
            String origin = stream.optString("url", "");
            int height = parseMaxResolution(stream.optString("resolutions", ""));
            if (origin.isEmpty() || height <= 0) continue;
            stream.put("url", DashServer.buildUrl(subjectId, season, episode, height, origin, stream.optLong("size", 0),
                    null));
            changed = true;
        }
        return changed;
    }

    private static int parseMaxResolution(String resolutions) {
        int best = 0;
        for (String part : resolutions.split(",")) {
            try {
                best = Math.max(best, Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }

    private static int parseQueryInt(Uri uri, String name) {
        try {
            return Integer.parseInt(uri.getQueryParameter(name));
        } catch (NumberFormatException missing) {
            return -1;
        }
    }

    private static boolean rewritePlaybackUrls(JSONObject node) throws JSONException {
        boolean changed = false;
        SignedResource resource = SignedResource.fromCookie(node.optString("signCookie", ""));
        if (resource != null) {
            for (String field : URL_FIELDS) {
                if (node.has(field)) {
                    node.put(field, resource.manifestUrl);
                    changed = true;
                }
            }
            if (changed) node.put("format", "DASH");
        }
        List<String> keys = new ArrayList<>();
        for (Iterator<String> it = node.keys(); it.hasNext(); ) keys.add(it.next());
        for (String key : keys) {
            Object value = node.opt(key);
            if (value instanceof JSONObject) changed |= rewritePlaybackUrls((JSONObject) value);
            else if (value instanceof JSONArray) changed |= rewritePlaybackUrls((JSONArray) value);
        }
        return changed;
    }

    private static boolean rewritePlaybackUrls(JSONArray array) throws JSONException {
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) changed |= rewritePlaybackUrls((JSONObject) value);
            else if (value instanceof JSONArray) changed |= rewritePlaybackUrls((JSONArray) value);
        }
        return changed;
    }

    static String rewriteResourceList(String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject data = root.optJSONObject("data");
            String subjectId = data == null ? "" : data.optString("subjectId", "");
            JSONArray list = data == null ? null : data.optJSONArray("list");
            if (subjectId.isEmpty() || list == null) return body;
            int routed = 0;
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item != null && route(subjectId, item)) routed++;
            }
            Log.i(TAG, "resource list " + subjectId + ": routed " + routed + " of " + list.length() + " items");
            return routed > 0 ? root.toString() : body;
        } catch (JSONException malformed) {
            Log.w(TAG, "resource list not rewritten: " + malformed.getMessage());
            return body;
        }
    }

    private static boolean route(String subjectId, JSONObject item) throws JSONException {
        if (!item.has("resourceLink")) return false;
        int season;
        int episode;
        int height;
        String origin;
        try {
            season = item.getInt("se");
            episode = item.getInt("ep");
            height = item.getInt("resolution");
            origin = item.getString("resourceLink");
        } catch (JSONException unusable) {
            Log.w(TAG, "resource item " + item.optString("resourceId") + " not routed: " + unusable.getMessage());
            return false;
        }
        if (season < 0 || episode < 0 || height <= 0) {
            Log.w(TAG, "resource item " + item.optString("resourceId") + " kept as is: se=" + season + " ep=" + episode
                    + " resolution=" + height);
            return false;
        }
        String resourceId = item.optString("resourceId", "");
        item.put("resourceLink", DashServer.buildUrl(subjectId, season, episode, height, origin, item.optLong("size", 0),
                resourceId.isEmpty() ? null : resourceId));
        return true;
    }

    static String rewriteMemberDays(String body) {
        try {
            JSONObject root = new JSONObject(body);
            return setMemberDays(root) ? root.toString() : body;
        } catch (JSONException malformed) {
            Log.w(TAG, "member info not rewritten: " + malformed.getMessage());
            return body;
        }
    }

    private static boolean setMemberDays(JSONObject node) throws JSONException {
        boolean changed = false;
        if (node.has("daysLeft") && node.has("memberType") && node.has("expiryDate")) {
            node.put("daysLeft", MEMBER_DAYS_LEFT);
            node.put("expiryDate", MEMBER_EXPIRY);
            changed = true;
        }
        List<String> keys = new ArrayList<>();
        for (Iterator<String> it = node.keys(); it.hasNext(); ) keys.add(it.next());
        for (String key : keys) {
            Object value = node.opt(key);
            if (value instanceof JSONObject) changed |= setMemberDays((JSONObject) value);
            else if (value instanceof JSONArray) changed |= setMemberDays((JSONArray) value);
        }
        return changed;
    }

    private static boolean setMemberDays(JSONArray array) throws JSONException {
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) changed |= setMemberDays((JSONObject) value);
            else if (value instanceof JSONArray) changed |= setMemberDays((JSONArray) value);
        }
        return changed;
    }
}

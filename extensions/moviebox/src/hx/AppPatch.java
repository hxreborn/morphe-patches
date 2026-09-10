/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String RESOURCE_LIST = BFF + "/subject-api/resource/v2";
    private static final String[] URL_FIELDS = {"url", "resourceLink", "downloadUrl", "playUrl"};
    private static final int MANAGER_RETRY_LIMIT = 200;
    private static final long MANAGER_RETRY_DELAY_MS = 50L;

    private AppPatch() {
    }

    public static void install(ClassLoader preferred) {
        final Handler main = new Handler(Looper.getMainLooper());
        main.post(new Runnable() {
            private int attempts;

            @Override
            public void run() {
                Class<?> manager = resolve(MANAGER_CLASS, preferred);
                if (manager != null) {
                    doInstall(manager);
                    return;
                }
                if (++attempts >= MANAGER_RETRY_LIMIT) {
                    Log.e(TAG, "network manager class not found");
                    return;
                }
                main.postDelayed(this, MANAGER_RETRY_DELAY_MS);
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

    private static void doInstall(Class<?> manager) {
        try {
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
            DashServer server = mainProcess() ? DashServer.bind(source) : null;
            try {
                if (server != null) server.start();
                Object wrapped = addInterceptor(client, interceptor(loader, source));
                clientField.set(instance, wrapped);
                callFactory.set(retrofit, wrapped);
            } catch (Exception e) {
                if (server != null) server.close();
                throw e;
            }
            Log.i(TAG, "DASH interceptor installed");
        } catch (Exception e) {
            Log.e(TAG, "cannot install DASH interceptor", e);
        }
    }

    private static boolean mainProcess() {
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

    private static Object interceptor(ClassLoader loader, MovieBoxSource source) throws Exception {
        Class<?> interceptorClass = Class.forName(OKHTTP_INTERCEPTOR, false, loader);
        return Proxy.newProxyInstance(loader, new Class<?>[]{interceptorClass}, new DashInterceptor(loader, source));
    }

    private static final class SignedResource {
        private static final String POLICY_KEY = "CloudFront-Policy=";
        private static final long EXPIRY_MARGIN_MS = 60_000L;

        final String cookie;
        final String manifestUrl;
        final long expiresAtMs;

        private SignedResource(String cookie, String manifestUrl, long expiresAtMs) {
            this.cookie = cookie;
            this.manifestUrl = manifestUrl;
            this.expiresAtMs = expiresAtMs;
        }

        static SignedResource fromCookie(String cookie) {
            String policy = cookieValue(cookie, POLICY_KEY);
            if (policy == null) return null;
            try {
                // CloudFront base64 alphabet
                String standard = policy.replace('-', '+').replace('_', '=').replace('~', '/');
                String json = new String(Base64.decode(standard, Base64.DEFAULT), StandardCharsets.UTF_8);
                JSONObject statement = new JSONObject(json).getJSONArray("Statement").getJSONObject(0);
                String resource = statement.getString("Resource");
                if (!resource.startsWith("https://") || !resource.endsWith("/*")) return null;
                long expiresAt = statement.getJSONObject("Condition").getJSONObject("DateLessThan")
                        .getLong("AWS:EpochTime") * 1000L;
                String manifestUrl = resource.substring(0, resource.length() - 2) + "/index.mpd";
                return new SignedResource(cookie, manifestUrl, expiresAt);
            } catch (JSONException | IllegalArgumentException malformed) {
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

        boolean expired() {
            return System.currentTimeMillis() + EXPIRY_MARGIN_MS >= expiresAtMs;
        }

        private static String cookieValue(String cookie, String key) {
            int at = cookie.indexOf(key);
            if (at < 0) return null;
            int start = at + key.length();
            int end = cookie.indexOf(';', start);
            return end < 0 ? cookie.substring(start) : cookie.substring(start, end);
        }
    }

    private static final class MovieBoxSource implements DashServer.Source {
        private static final int FILE_RETENTION = 8;
        private static final int RESOURCE_RETENTION = 32;
        private static final int ORIGIN_PROBE_TIMEOUT_MS = 10000;

        private final ClassLoader loader;
        private final Object client;
        private final String apiBase;
        private final Map<String, SignedResource> resources = lru(RESOURCE_RETENTION);
        private final Map<String, Long> originLengths = lru(RESOURCE_RETENTION);
        private final Map<String, FutureTask<DashFile>> files = lru(FILE_RETENTION);

        MovieBoxSource(ClassLoader loader, Object client, String apiBase) {
            this.loader = loader;
            this.client = client;
            this.apiBase = apiBase.endsWith("/") ? apiBase : apiBase + "/";
        }

        SignedResource observePlayInfo(String requestUrl, String body) {
            String key = SignedResource.key(Uri.parse(requestUrl));
            SignedResource resource = SignedResource.fromPlayInfo(body);
            Log.i(TAG, "play-info " + key + ": " + (resource == null ? "no signed DASH resource" : "DASH"));
            if (resource == null) Log.d(TAG, "play-info body " + body);
            if (key == null || resource == null) return null;
            synchronized (resources) {
                resources.put(key, resource);
            }
            return resource;
        }

        @Override
        public DashFile open(final String subjectId, final int season, final int episode, final int height,
                             String origin, long originSize) throws IOException {
            String label = SignedResource.key(subjectId, season, episode) + "@" + height;
            if (origin != null && originSize > 0 && originLength(origin) == originSize) {
                Log.i(TAG, label + ": origin serves the advertised " + originSize + " bytes");
                return null;
            }
            try {
                resource(subjectId, season, episode, null);
            } catch (IOException noResource) {
                if (origin == null) throw noResource;
                Log.i(TAG, label + ": falling back to origin, " + noResource.getMessage());
                return null;
            }
            return getOrCreateFile(subjectId, season, episode, height);
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
                            String manifestUrl = resource(subjectId, season, episode, null).manifestUrl;
                            return DashFile.open(manifestUrl, height, new DashFile.Cookies() {
                                @Override
                                public String current() throws IOException {
                                    return resource(subjectId, season, episode, null).cookie;
                                }

                                @Override
                                public String refresh(String rejected) throws IOException {
                                    return resource(subjectId, season, episode, rejected).cookie;
                                }
                            });
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

        private SignedResource resource(String subjectId, int season, int episode, String rejectedCookie)
                throws IOException {
            String key = SignedResource.key(subjectId, season, episode);
            SignedResource cached;
            synchronized (resources) {
                cached = resources.get(key);
            }
            if (cached != null && !cached.expired() && !cached.cookie.equals(rejectedCookie)) return cached;
            String url = apiBase + PLAY_INFO + "?subjectId=" + subjectId + "&se=" + season + "&ep=" + episode
                    + "&isVip=true";
            SignedResource fetched = observePlayInfo(url, get(url));
            if (fetched == null) throw new IOException("play-info has no signed DASH resource for " + key);
            return fetched;
        }

        private String get(String url) throws IOException {
            try {
                Object builder = Class.forName("okhttp3.Request$Builder", false, loader).getConstructor().newInstance();
                builder.getClass().getMethod("url", String.class).invoke(builder, url);
                Object request = builder.getClass().getMethod("build").invoke(builder);
                Object call = client.getClass().getMethod("newCall", request.getClass()).invoke(client, request);
                Object response = call.getClass().getMethod("execute").invoke(call);
                Object body = response.getClass().getMethod("body").invoke(response);
                return (String) body.getClass().getMethod("string").invoke(body);
            } catch (InvocationTargetException e) {
                throw new IOException(e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new IOException(e);
            }
        }

        private static <V> Map<String, V> lru(final int capacity) {
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

        DashInterceptor(ClassLoader loader, MovieBoxSource source) {
            this.loader = loader;
            this.source = source;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (args == null || args.length == 0) {
                if ("toString".equals(name)) return "MovieBoxDashInterceptor";
                if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            }
            if ("equals".equals(name) && args != null && args.length == 1) return proxy == args[0];
            if (!"intercept".equals(name)) return null;

            try {
                return intercept(args[0]);
            } catch (InvocationTargetException wrapped) {
                throw wrapped.getCause() != null ? wrapped.getCause() : wrapped;
            }
        }

        private Object intercept(Object chain) throws Throwable {
            Object request = chain.getClass().getMethod("request").invoke(chain);
            Object response = chain.getClass().getMethod("proceed", request.getClass()).invoke(chain, request);

            Object httpUrl = request.getClass().getMethod("url").invoke(request);
            String url = String.valueOf(httpUrl);
            boolean playInfo = url.contains(PLAY_INFO);
            if (!playInfo && !url.contains(RESOURCE_LIST)) return response;

            Object body = response.getClass().getMethod("body").invoke(response);
            if (body == null) return response;

            byte[] bytes = (byte[]) body.getClass().getMethod("bytes").invoke(body);
            String text = new String(bytes, StandardCharsets.UTF_8);
            String out;
            if (playInfo) {
                source.observePlayInfo(url, text);
                out = rewritePlayInfo(text);
            } else {
                out = rewriteResourceList(text);
            }
            byte[] payload = out.equals(text) ? bytes : out.getBytes(StandardCharsets.UTF_8);
            return withBody(response, body, payload);
        }

        private Object withBody(Object response, Object body, byte[] payload) throws Throwable {
            Object mediaType = body.getClass().getMethod("contentType").invoke(body);
            Object newBody = responseBody(mediaType, payload);

            Object builder = response.getClass().getMethod("newBuilder").invoke(response);
            Class<?> bodyClass = Class.forName("okhttp3.ResponseBody", false, loader);
            builder.getClass().getMethod("body", bodyClass).invoke(builder, newBody);
            builder.getClass().getMethod("removeHeader", String.class).invoke(builder, "Content-Length");
            return builder.getClass().getMethod("build").invoke(builder);
        }

        private Object responseBody(Object mediaType, byte[] bytes) throws Throwable {
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

    static String rewritePlayInfo(String body) {
        try {
            JSONObject root = new JSONObject(body);
            return walk(root) ? root.toString() : body;
        } catch (JSONException malformed) {
            return body;
        }
    }

    private static boolean walk(JSONObject node) throws JSONException {
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
            if (value instanceof JSONObject) changed |= walk((JSONObject) value);
            else if (value instanceof JSONArray) changed |= walk((JSONArray) value);
        }
        return changed;
    }

    private static boolean walk(JSONArray array) throws JSONException {
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) changed |= walk((JSONObject) value);
            else if (value instanceof JSONArray) changed |= walk((JSONArray) value);
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
            Log.w(TAG, "resource item " + item.optString("resourceId") + " kept as is: " + unusable.getMessage());
            return false;
        }
        if (season < 0 || episode < 0 || height <= 0) {
            Log.w(TAG, "resource item " + item.optString("resourceId") + " kept as is: se=" + season + " ep=" + episode
                    + " resolution=" + height);
            return false;
        }
        item.put("resourceLink", DashServer.url(subjectId, season, episode, height, origin, item.optLong("size", 0)));
        return true;
    }
}

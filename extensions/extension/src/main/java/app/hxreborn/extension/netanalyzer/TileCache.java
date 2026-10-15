/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

final class TileCache {

    private static final String URL_TEMPLATE = "https://tile.openstreetmap.org/%d/%d/%d.png";

    private static final String USER_AGENT = "NetworkAnalyzerPatches/1.0 (+https://github.com/hxreborn/morphe-patches)";

    private static final String KEY_FORMAT = "%d/%d/%d";

    private static final String LINE_BREAK = "\n";

    private static final String FAILURE_MESSAGE = "Tile request failed with HTTP ";

    private static final String STORE_MESSAGE = "Cannot store tile ";

    private static final String DISK_DIRECTORY = "osm";

    private static final String TILE_EXTENSION = ".png";

    private static final String META_EXTENSION = ".meta";

    private static final String TEMPORARY_EXTENSION = ".tmp";

    private static final String HEADER_USER_AGENT = "User-Agent";

    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    private static final String HEADER_ETAG = "ETag";

    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    private static final String MAX_AGE_PREFIX = "max-age=";

    private static final String DIRECTIVE_SEPARATOR = ",";

    private static final int CONNECT_TIMEOUT_MILLIS = 8000;

    private static final int READ_TIMEOUT_MILLIS = 10000;

    private static final int MAX_CONCURRENT_LOADS = 4;

    private static final int COPY_BUFFER_BYTES = 8192;

    private static final int BYTES_PER_KILOBYTE = 1024;

    private static final int MEMORY_FRACTION = 8;

    private static final long MILLIS_PER_SECOND = 1000L;

    private static final long DEFAULT_TTL_MILLIS = TimeUnit.DAYS.toMillis(7);

    private static final long FAILURE_COOLDOWN_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final LruCache<String, Bitmap> memory;

    private final File root;

    private final Listener listener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_CONCURRENT_LOADS, MAX_CONCURRENT_LOADS, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    private final Map<String, FutureTask<?>> pending = new ConcurrentHashMap<>();

    private final Map<String, Long> failedAt = new ConcurrentHashMap<>();

    TileCache(Context context, Listener listener) {
        this.listener = listener;
        this.root = new File(context.getCacheDir(), DISK_DIRECTORY);
        final int memoryKilobytes = (int) (Runtime.getRuntime().maxMemory() / BYTES_PER_KILOBYTE / MEMORY_FRACTION);
        this.memory = new LruCache<String, Bitmap>(memoryKilobytes) {

            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / BYTES_PER_KILOBYTE;
            }

        };
    }

    static String key(int zoom, int x, int y) {
        return String.format(Locale.ROOT, KEY_FORMAT, zoom, x, y);
    }

    Bitmap get(int zoom, int x, int y) {
        return this.memory.get(key(zoom, x, y));
    }

    void request(int zoom, int x, int y) {
        final String key = key(zoom, x, y);
        if (this.executor.isShutdown() || this.memory.get(key) != null || this.pending.containsKey(key)) {
            return;
        }
        final Long failure = this.failedAt.get(key);
        if (failure != null && System.currentTimeMillis() - failure < FAILURE_COOLDOWN_MILLIS) {
            return;
        }
        final FutureTask<Void> task = new FutureTask<>(() -> load(key, zoom, x, y), null);
        this.pending.put(key, task);
        this.executor.execute(task);
    }

    void retain(Set<String> wanted) {
        for (Map.Entry<String, FutureTask<?>> entry : this.pending.entrySet()) {
            if (!wanted.contains(entry.getKey())) {
                this.pending.remove(entry.getKey());
                entry.getValue().cancel(false);
                this.executor.remove(entry.getValue());
            }
        }
    }

    void release() {
        this.executor.shutdownNow();
        this.pending.clear();
        this.mainHandler.removeCallbacksAndMessages(null);
    }

    private void load(String key, int zoom, int x, int y) {
        try {
            final File file = tileFile(zoom, x, y);
            final File metaFile = metaFile(file);
            final Meta meta = file.isFile() ? readMeta(metaFile) : null;
            Bitmap bitmap = null;
            if (meta != null && meta.expiresAt > System.currentTimeMillis()) {
                bitmap = decode(file);
            }
            if (bitmap == null) {
                try {
                    revalidate(zoom, x, y, file, metaFile, meta);
                } catch (IOException ex) {
                    this.failedAt.put(key, System.currentTimeMillis());
                }
                bitmap = decode(file);
            }
            if (bitmap != null) {
                this.failedAt.remove(key);
                this.memory.put(key, bitmap);
                this.mainHandler.post(this.listener::onTileLoaded);
            } else {
                this.failedAt.put(key, System.currentTimeMillis());
            }
        } finally {
            this.pending.remove(key);
        }
    }

    private void revalidate(int zoom, int x, int y, File file, File metaFile, Meta meta) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(
                String.format(Locale.ROOT, URL_TEMPLATE, zoom, x, y))
            .openConnection();
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestProperty(HEADER_USER_AGENT, USER_AGENT);
            if (meta != null && !meta.etag.isEmpty()) {
                connection.setRequestProperty(HEADER_IF_NONE_MATCH, meta.etag);
            }
            final int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                writeFile(file, connection);
            } else if (status != HttpURLConnection.HTTP_NOT_MODIFIED || meta == null) {
                throw new IOException(FAILURE_MESSAGE + status);
            }
            final String etag = connection.getHeaderField(HEADER_ETAG);
            writeMeta(metaFile, new Meta((etag != null) ? etag : "",
                    System.currentTimeMillis() + ttlMillis(connection.getHeaderField(HEADER_CACHE_CONTROL))));
        } finally {
            connection.disconnect();
        }
    }

    private static long ttlMillis(String cacheControl) {
        if (cacheControl != null) {
            for (String directive : cacheControl.split(DIRECTIVE_SEPARATOR)) {
                final String trimmed = directive.trim();
                if (trimmed.startsWith(MAX_AGE_PREFIX)) {
                    try {
                        return Long.parseLong(trimmed.substring(MAX_AGE_PREFIX.length())) * MILLIS_PER_SECOND;
                    } catch (NumberFormatException ex) {
                        return DEFAULT_TTL_MILLIS;
                    }
                }
            }
        }
        return DEFAULT_TTL_MILLIS;
    }

    private static void writeFile(File file, HttpURLConnection connection) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream input = connection.getInputStream()) {
            final byte[] buffer = new byte[COPY_BUFFER_BYTES];
            int read;
            while ((read = input.read(buffer)) != -1) {
                bytes.write(buffer, 0, read);
            }
        }
        final File temporary = new File(file.getPath() + TEMPORARY_EXTENSION);
        file.getParentFile().mkdirs();
        try (OutputStream output = new FileOutputStream(temporary)) {
            bytes.writeTo(output);
        }
        if (!temporary.renameTo(file)) {
            throw new IOException(STORE_MESSAGE + file);
        }
    }

    private static Bitmap decode(File file) {
        if (!file.isFile()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            final Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) {
                file.delete();
            }
            return bitmap;
        } catch (IOException ex) {
            return null;
        }
    }

    private static Meta readMeta(File metaFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            final String etag = reader.readLine();
            final String expiresAt = reader.readLine();
            if (etag == null || expiresAt == null) {
                return null;
            }
            return new Meta(etag, Long.parseLong(expiresAt));
        } catch (IOException | NumberFormatException ex) {
            return null;
        }
    }

    private static void writeMeta(File metaFile, Meta meta) throws IOException {
        try (FileWriter writer = new FileWriter(metaFile)) {
            writer.write(meta.etag + LINE_BREAK + meta.expiresAt + LINE_BREAK);
        }
    }

    private File tileFile(int zoom, int x, int y) {
        return new File(new File(new File(this.root, String.valueOf(zoom)), String.valueOf(x)), y + TILE_EXTENSION);
    }

    private static File metaFile(File tileFile) {
        final String path = tileFile.getPath();
        return new File(path.substring(0, path.length() - TILE_EXTENSION.length()) + META_EXTENSION);
    }

    private static final class Meta {

        final String etag;

        final long expiresAt;

        Meta(String etag, long expiresAt) {
            this.etag = etag;
            this.expiresAt = expiresAt;
        }

    }

    interface Listener {

        void onTileLoaded();

    }

}

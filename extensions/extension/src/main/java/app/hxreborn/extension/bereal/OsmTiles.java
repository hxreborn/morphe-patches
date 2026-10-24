/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

final class OsmTiles {

    private static final String TAG = "OsmTiles";

    private static final String URL_PREFIX = "https://tile.openstreetmap.org/";

    private static final String USER_AGENT = "hxreborn-patches-location-picker/1.0 "
            + "(+https://github.com/hxreborn/morphe-patches)";

    private static final int CACHE_BYTES = 12 * 1024 * 1024;

    private static final int THREADS = 3;

    private static final int TIMEOUT_MS = 8000;

    private static final long RETRY_AFTER_MS = 20000;

    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(CACHE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private ExecutorService executor;

    private final Set<String> pending = Collections.synchronizedSet(new HashSet<>());

    private final Map<String, Long> failed = new HashMap<>();

    private final Listener listener;

    private volatile int[] window = new int[] { 0, 0, 0, 0, 0 };

    OsmTiles(Listener listener) {
        this.listener = listener;
    }

    void setWindow(int zoom, int firstX, int lastX, int firstY, int lastY) {
        this.window = new int[] { zoom, firstX, lastX, firstY, lastY };
    }

    Bitmap get(int zoom, int x, int y) {
        return this.cache.get(key(zoom, x, y));
    }

    void request(int zoom, int x, int y) {
        final String key = key(zoom, x, y);
        synchronized (this.failed) {
            final Long failedAt = this.failed.get(key);
            if (failedAt != null && System.currentTimeMillis() - failedAt < RETRY_AFTER_MS) {
                return;
            }
        }
        if (!this.pending.add(key)) {
            return;
        }
        executor().execute(() -> load(key, zoom, x, y));
    }

    void shutdown() {
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
        this.pending.clear();
    }

    private ExecutorService executor() {
        if (this.executor == null) {
            this.executor = Executors.newFixedThreadPool(THREADS);
        }
        return this.executor;
    }

    private void load(String key, int zoom, int x, int y) {
        try {
            if (!wanted(zoom, x, y)) {
                return;
            }
            final Bitmap tile = download(zoom, x, y);
            if (tile == null) {
                synchronized (this.failed) {
                    this.failed.put(key, System.currentTimeMillis());
                }
                return;
            }
            this.cache.put(key, tile);
            this.listener.onTileLoaded();
        } finally {
            this.pending.remove(key);
        }
    }

    private boolean wanted(int zoom, int x, int y) {
        final int[] current = this.window;
        final int count = 1 << zoom;
        final int offset = wrap(x - current[1], count);
        return zoom == current[0] && offset <= current[2] - current[1] && y >= current[3] && y <= current[4];
    }

    private static Bitmap download(int zoom, int x, int y) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(URL_PREFIX + zoom + "/" + x + "/" + y + ".png").openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input, null, options);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Tile not loaded: " + zoom + "/" + x + "/" + y, ex);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static int wrap(int value, int count) {
        final int remainder = value % count;
        return (remainder < 0) ? remainder + count : remainder;
    }

    private static String key(int zoom, int x, int y) {
        return zoom + "/" + x + "/" + y;
    }

    interface Listener {

        void onTileLoaded();

    }

}

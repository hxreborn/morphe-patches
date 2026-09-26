/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.terabox;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class TeraboxHdUnlock {

    private static final String TAG = "TeraboxHdUnlock";
    private static final int FIRST_HD_HEIGHT = 720;
    private static final int RECORDED_FILES = 32;

    private static final Map<String, String> originalLinks = new LinkedHashMap<String, String>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > RECORDED_FILES;
        }
    };
    private static volatile String streamingPath;
    private static volatile boolean playingOriginal;

    private TeraboxHdUnlock() {
    }

    public static void recordOriginalFile(String path, String dlink) {
        if (path != null && dlink != null && !dlink.isEmpty()) {
            synchronized (originalLinks) {
                originalLinks.put(path, dlink);
            }
        }
    }

    public static String selectPlaybackUrl(Object vastView, String url) {
        playingOriginal = false;
        streamingPath = null;
        if (url == null || !url.contains("/api/streaming")) {
            return url;
        }
        Uri uri = Uri.parse(url);
        streamingPath = uri.getQueryParameter("path");
        String dlink = originalLink(streamingPath);
        if (dlink == null || !isHdTier(uri.getQueryParameter("type"))) {
            return url;
        }
        String proxyUrl;
        try {
            proxyUrl = ParallelRangeProxy.proxyUrl(dlink);
        } catch (IOException exception) {
            Log.w(TAG, "Could not start the stream proxy", exception);
            return url;
        }
        disablePlayerOption(vastView, "setEnableDashP2P");
        disablePlayerOption(vastView, "setEnableCustomHls");
        playingOriginal = true;
        return proxyUrl;
    }

    public static boolean requiresReload(Enum<?> resolution) {
        return isHdTier(resolution.name()) ? originalLink(streamingPath) != null : playingOriginal;
    }

    private static String originalLink(String path) {
        if (path == null) {
            return null;
        }
        synchronized (originalLinks) {
            return originalLinks.get(path);
        }
    }

    private static boolean isHdTier(String tier) {
        if (tier == null) {
            return false;
        }
        String label = tier.substring(tier.lastIndexOf('_') + 1);
        if (label.endsWith("K") || label.equals("ORIGIN")) {
            return true;
        }
        int digits = 0;
        while (digits < label.length() && Character.isDigit(label.charAt(digits))) {
            digits++;
        }
        return digits != 0 && Integer.parseInt(label.substring(0, digits)) >= FIRST_HD_HEIGHT;
    }

    private static void disablePlayerOption(Object vastView, String setter) {
        try {
            vastView.getClass().getMethod(setter, boolean.class).invoke(vastView, false);
        } catch (ReflectiveOperationException exception) {
            Log.w(TAG, "Could not call " + setter, exception);
        }
    }
}

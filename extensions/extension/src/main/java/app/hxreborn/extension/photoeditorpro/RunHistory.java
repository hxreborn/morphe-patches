/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RunHistory {

    private static final int KEEP = 5;
    private static final long FALLBACK_MS = 9000;

    private static final Object LOCK = new Object();
    private static Map<String, List<Long>> cache;

    private RunHistory() {
    }

    public static void add(String feature, long millis) {
        if (feature == null || millis <= 0 || millis > 5 * 60_000) {
            return;
        }
        synchronized (LOCK) {
            Map<String, List<Long>> all = load();
            List<Long> runs = all.get(feature);
            if (runs == null) {
                runs = new ArrayList<>();
                all.put(feature, runs);
            }
            runs.add(millis);
            while (runs.size() > KEEP) {
                runs.remove(0);
            }
            PatchSettings.RUN_HISTORY.save(encode(all));
        }
    }

    public static long expected(String feature) {
        synchronized (LOCK) {
            List<Long> runs = load().get(feature);
            if (runs == null || runs.isEmpty()) {
                return FALLBACK_MS;
            }
            List<Long> sorted = new ArrayList<>(runs);
            Collections.sort(sorted);
            return sorted.get(sorted.size() / 2);
        }
    }

    private static Map<String, List<Long>> load() {
        if (cache != null) {
            return cache;
        }
        cache = new LinkedHashMap<>();
        String raw = PatchSettings.RUN_HISTORY.get();
        if (raw == null || raw.isEmpty()) {
            return cache;
        }
        for (String block : raw.split(";")) {
            int split = block.indexOf('=');
            if (split <= 0) {
                continue;
            }
            List<Long> runs = new ArrayList<>();
            for (String value : block.substring(split + 1).split(",")) {
                try {
                    runs.add(Long.parseLong(value.trim()));
                } catch (NumberFormatException ex) {
                    // A corrupt entry must not discard the rest of the history
                }
            }
            if (!runs.isEmpty()) {
                cache.put(block.substring(0, split), runs);
            }
        }
        return cache;
    }

    private static String encode(Map<String, List<Long>> all) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, List<Long>> entry : all.entrySet()) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append(entry.getKey()).append('=');
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(entry.getValue().get(i));
            }
        }
        return out.toString();
    }
}

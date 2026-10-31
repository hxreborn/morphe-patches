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

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.S_V2)
public final class MemoryFiles {

    private static final DateTimeFormatter BASE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);

    private MemoryFiles() {
    }

    /** Capture time, else post time, else 0. */
    public static long time(final long takenAt, final long postedAt) {
        return (takenAt > 0) ? takenAt : Math.max(postedAt, 0);
    }

    /** Time-based name, or the post id when the post has no time. */
    public static String base(final long takenAt, final long postedAt, final String postId, final ZoneId zone) {
        final long time = time(takenAt, postedAt);
        return (time > 0) ? BASE.format(Instant.ofEpochMilli(time).atZone(zone))
                : postId.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public static String suffixed(final String base, final int attempt) {
        return (attempt <= 1) ? base : base + "_" + attempt;
    }

    public static String extension(final String url, final String fallback) {
        String path;
        try {
            path = URI.create(url).getPath();
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
        if (path == null) {
            return fallback;
        }
        final int dot = path.lastIndexOf('.');
        final String ext = (dot < 0) ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "jpeg":
            case "jpg":
                return "jpg";
            case "webp":
            case "png":
            case "mp4":
                return ext;
            default:
                return fallback;
        }
    }

    public static String mimeType(final String ext) {
        switch (ext) {
            case "jpg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "mp4":
                return "video/mp4";
            default:
                return "image/webp";
        }
    }
}

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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.S_V2)
public final class ExifValues {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.ROOT);

    private static final DateTimeFormatter OFFSET = DateTimeFormatter.ofPattern("xxx", Locale.ROOT);

    private ExifValues() {
    }

    public static String dateTime(final long takenAt, final ZoneId zone) {
        return DATE_TIME.format(Instant.ofEpochMilli(takenAt).atZone(zone));
    }

    public static String offset(final long takenAt, final ZoneId zone) {
        return OFFSET.format(Instant.ofEpochMilli(takenAt).atZone(zone));
    }

    /** The caption when EXIF can hold it, else null (the platform ExifInterface writes ASCII only). */
    public static String description(final String caption) {
        if (caption == null || caption.isEmpty()) {
            return null;
        }
        for (int i = 0; i < caption.length(); i++) {
            final char c = caption.charAt(i);
            if (c > 0x7E || (c < 0x20 && c != '\n' && c != '\t')) {
                return null;
            }
        }
        return caption;
    }

    public static String rational(final double degrees) {
        final long hundredths = Math.round(Math.abs(degrees) * 360000);
        return (hundredths / 360000) + "/1," + (hundredths % 360000 / 6000) + "/1," + (hundredths % 6000) + "/100";
    }

    public static String latitudeRef(final double latitude) {
        return (latitude < 0) ? "S" : "N";
    }

    public static String longitudeRef(final double longitude) {
        return (longitude < 0) ? "W" : "E";
    }
}

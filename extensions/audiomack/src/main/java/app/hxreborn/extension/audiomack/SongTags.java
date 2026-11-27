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
package app.hxreborn.extension.audiomack;

import java.util.regex.Pattern;

final class SongTags {

    private static final Pattern UNSAFE_FILE_NAME_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

    private static final int MAX_FILE_NAME_LENGTH = 120;

    final String title;

    final String artist;

    final String album;

    final byte[] coverJpeg;

    private final String id;

    SongTags(String id, String title, String artist, String album, byte[] coverJpeg) {
        this.id = id;
        this.title = trimmed(title);
        this.artist = trimmed(artist);
        this.album = trimmed(album);
        this.coverJpeg = coverJpeg;
    }

    String fileName() {
        final String name;
        if (this.title.isEmpty()) {
            name = (this.id != null) ? this.id : "";
        } else if (this.artist.isEmpty()) {
            name = this.title;
        } else {
            name = this.artist + " - " + this.title;
        }
        String safe = UNSAFE_FILE_NAME_CHARACTERS.matcher(name).replaceAll("_").trim();
        if (safe.length() > MAX_FILE_NAME_LENGTH) {
            safe = safe.substring(0, MAX_FILE_NAME_LENGTH).trim();
        }
        while (safe.startsWith(".")) {
            safe = safe.substring(1);
        }
        return safe.isEmpty() ? "Audiomack" : safe;
    }

    private static String trimmed(String value) {
        return (value != null) ? value.trim() : "";
    }

}

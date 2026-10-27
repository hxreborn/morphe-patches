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
package app.hxreborn.extension.castle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import app.hxreborn.extension.shared.Streams;

@SuppressWarnings("unused")
public final class PreviewPlaylist {

    private static final String PREVIEW_PATH = "/index_preview";

    private static final String DURATION_TAG = "#EXTINF:";

    private static final Pattern NUMBERED_SEGMENT = Pattern.compile("^(.*/)(\\d+)(\\.[^/?]+(?:\\?.*)?)$");

    private static final int TIMEOUT_MILLIS = 10000;

    private PreviewPlaylist() {

    }

    public static InputStream expand(Uri uri, InputStream playlist) throws IOException {
        return expand(uri.toString(), playlist);
    }

    public static InputStream expand(String location, InputStream playlist) throws IOException {
        if (location == null || !location.contains(PREVIEW_PATH)) {
            return FullHdPlaylist.expand(playlist);
        }
        byte[] original = Streams.readAll(playlist);
        String expanded;
        try {
            expanded = expand(new String(original, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            expanded = null;
        }
        return new ByteArrayInputStream((expanded != null) ? expanded.getBytes(StandardCharsets.UTF_8) : original);
    }

    static String expand(String playlist) throws IOException {
        Padding padding = Padding.of(playlist);
        if (padding == null) {
            return null;
        }
        int lastSegment = lastAvailable(padding);
        return (lastSegment > padding.lastServed) ? padding.expanded(lastSegment) : null;
    }

    private static int lastAvailable(Padding padding) throws IOException {
        int available = padding.lastServed;
        int missing = padding.lastServed + (int) Math.ceil(padding.seconds) + 1;
        if (exists(padding.segmentUrl(missing))) {
            throw new IOException("Segment " + missing + " exists past the padded duration");
        }
        while (missing - available > 1) {
            int middle = (available + missing) >>> 1;
            if (exists(padding.segmentUrl(middle))) {
                available = middle;
            } else {
                missing = middle;
            }
        }
        return available;
    }

    static boolean exists(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("Range", "bytes=0-0");
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL) {
                return true;
            }
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return false;
            }
            throw new IOException("HTTP " + code + " for " + url);
        } finally {
            connection.disconnect();
        }
    }

    static final class Padding {

        final int lastServed;

        final double seconds;

        private final String[] lines;

        private final int durationLine;

        private final int segmentLine;

        private final String prefix;

        private final String suffix;

        private Padding(String[] lines, int durationLine, int segmentLine, Matcher segment, double seconds) {
            this.lines = lines;
            this.durationLine = durationLine;
            this.segmentLine = segmentLine;
            this.prefix = segment.group(1);
            this.suffix = segment.group(3);
            this.lastServed = Integer.parseInt(segment.group(2));
            this.seconds = seconds;
        }

        static Padding of(String playlist) {
            String[] lines = playlist.replace("\r", "").split("\n", -1);
            int last = -1;
            int previous = -1;
            for (int index = 0; index < lines.length; index++) {
                if (!lines[index].isEmpty() && !lines[index].startsWith("#")) {
                    previous = last;
                    last = index;
                }
            }
            if (previous < 0 || !lines[last].equals(lines[previous]) || !lines[last - 1].startsWith(DURATION_TAG)) {
                return null;
            }
            Matcher segment = NUMBERED_SEGMENT.matcher(lines[last]);
            if (!segment.matches()) {
                return null;
            }
            String duration = lines[last - 1].substring(DURATION_TAG.length());
            int comma = duration.indexOf(',');
            double seconds = Double.parseDouble((comma >= 0) ? duration.substring(0, comma) : duration);
            return new Padding(lines, last - 1, last, segment, seconds);
        }

        String segmentUrl(int number) {
            return this.prefix + number + this.suffix;
        }

        String expanded(int lastSegment) {
            StringBuilder playlist = new StringBuilder();
            for (int index = 0; index < this.durationLine; index++) {
                playlist.append(this.lines[index]).append('\n');
            }
            String duration = String.format(Locale.ROOT, "%s%.6f,", DURATION_TAG,
                    this.seconds / (lastSegment - this.lastServed));
            for (int number = this.lastServed + 1; number <= lastSegment; number++) {
                playlist.append(duration).append('\n').append(segmentUrl(number)).append('\n');
            }
            for (int index = this.segmentLine + 1; index < this.lines.length; index++) {
                playlist.append(this.lines[index]);
                if (index < this.lines.length - 1) {
                    playlist.append('\n');
                }
            }
            return playlist.toString();
        }

    }

}

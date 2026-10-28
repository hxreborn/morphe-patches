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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.hxreborn.extension.shared.Streams;

@SuppressWarnings("unused")
public final class FullHdPlaylist {

    private static final int HD = 2;

    private static final int FULL_HD = 3;

    private static final Pattern HD_DIRECTORY = Pattern.compile("([/_])720(ts)?/");

    private static volatile boolean requested;

    private FullHdPlaylist() {

    }

    public static Integer resolution(Integer resolution) {
        if (resolution != null) {
            requested = resolution == FULL_HD;
        }
        return requested ? Integer.valueOf(HD) : resolution;
    }

    public static InputStream expand(InputStream playlist) throws IOException {
        if (!requested) {
            return playlist;
        }
        byte[] original = Streams.readAll(playlist);
        String fullHd;
        try {
            fullHd = fullHd(new String(original, StandardCharsets.UTF_8), new UrlExistence() {

                @Override
                public boolean exists(String url) throws IOException {
                    return PreviewPlaylist.exists(url);
                }

            });
        } catch (IOException | RuntimeException ex) {
            fullHd = null;
        }
        return new ByteArrayInputStream((fullHd != null) ? fullHd.getBytes(StandardCharsets.UTF_8) : original);
    }

    static String fullHd(String playlist, UrlExistence existence) throws IOException {
        String[] lines = playlist.replace("\r", "").split("\n", -1);
        List<Integer> segments = new ArrayList<>();
        for (int index = 0; index < lines.length; index++) {
            if (!lines[index].isEmpty() && !lines[index].startsWith("#")) {
                segments.add(index);
            }
        }
        if (segments.isEmpty() || fullHdUrl(lines[segments.get(0)]) == null) {
            return null;
        }
        int count = availableSegments(lines, segments, existence);
        if (count == 0) {
            return null;
        }
        int lastLine = segments.get(count - 1);
        StringBuilder rewritten = new StringBuilder();
        for (int index = 0; index <= lastLine; index++) {
            boolean segment = !lines[index].isEmpty() && !lines[index].startsWith("#");
            rewritten.append(segment ? fullHdUrl(lines[index]) : lines[index]).append('\n');
        }
        for (int index = segments.get(segments.size() - 1) + 1; index < lines.length; index++) {
            rewritten.append(lines[index]);
            if (index < lines.length - 1) {
                rewritten.append('\n');
            }
        }
        return rewritten.toString();
    }

    private static int availableSegments(String[] lines, List<Integer> segments, UrlExistence existence)
            throws IOException {
        if (!existence.exists(fullHdUrl(lines[segments.get(0)]))) {
            return 0;
        }
        int missing = segments.size() - 1;
        if (existence.exists(fullHdUrl(lines[segments.get(missing)]))) {
            return segments.size();
        }
        int available = 0;
        while (missing - available > 1) {
            int middle = (available + missing) >>> 1;
            if (existence.exists(fullHdUrl(lines[segments.get(middle)]))) {
                available = middle;
            } else {
                missing = middle;
            }
        }
        return available + 1;
    }

    static String fullHdUrl(String segmentUrl) {
        Matcher directory = HD_DIRECTORY.matcher(segmentUrl);
        if (!directory.find()) {
            return null;
        }
        String suffix = (directory.group(2) != null) ? directory.group(2) : "";
        return segmentUrl.substring(0, directory.start()) + directory.group(1) + "1080" + suffix + "/"
                + segmentUrl.substring(directory.end());
    }

}

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
package hx.loopback;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

final class LoopbackRequest {

    static final String ORIGIN_PARAMETER = "origin";

    static final String ORIGIN_SIZE_PARAMETER = "size";

    static final String RESOURCE_ID_PARAMETER = "resourceId";

    private static final int MAX_HEAD_BYTES = 8192;

    private static final byte[] HEAD_END = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);

    private static final String RANGE_HEADER = "Range:";

    private static final Pattern PATH = Pattern.compile("^/dash/(\\d{1,19})/(\\d{1,9})/(\\d{1,9})/(\\d{1,9})\\.mp4$");

    private static final char DEL = 0x7F;

    final boolean headOnly;

    final String target;

    final String subjectId;

    final int season;

    final int episode;

    final int height;

    final String origin;

    final long originSize;

    final String resourceId;

    final RangeHeader range;

    private LoopbackRequest(boolean headOnly, String target, Matcher path, Uri uri, RangeHeader range) {
        this.headOnly = headOnly;
        this.target = target;
        this.subjectId = path.group(1);
        this.season = Integer.parseInt(path.group(2));
        this.episode = Integer.parseInt(path.group(3));
        this.height = Integer.parseInt(path.group(4));
        this.origin = validOrigin(uri.getQueryParameter(ORIGIN_PARAMETER));
        this.originSize = parseSize(uri.getQueryParameter(ORIGIN_SIZE_PARAMETER));
        this.resourceId = uri.getQueryParameter(RESOURCE_ID_PARAMETER);
        this.range = range;
    }

    static String readHead(InputStream in) throws IOException {
        byte[] head = new byte[MAX_HEAD_BYTES];
        int length = 0;
        while (length < head.length) {
            int next = in.read();
            if (next < 0) {
                break;
            }
            head[length++] = (byte) next;
            if (endsWithHeadEnd(head, length)) {
                return new String(head, 0, length - HEAD_END.length, StandardCharsets.ISO_8859_1);
            }
        }
        throw new IOException("request head missing or over " + MAX_HEAD_BYTES + " bytes");
    }

    static LoopbackRequest parse(String head) {
        String[] lines = head.split("\r\n");
        String[] request = lines[0].split(" ");
        if (request.length < 2) {
            return null;
        }
        boolean headOnly = "HEAD".equals(request[0]);
        Uri uri = Uri.parse("http://127.0.0.1" + request[1]);
        Matcher path = (uri.getPath() != null) ? PATH.matcher(uri.getPath()) : null;
        if (path == null || !path.matches() || !(headOnly || "GET".equals(request[0]))) {
            return null;
        }
        return new LoopbackRequest(headOnly, request[1], path, uri, RangeHeader.parse(findRange(lines)));
    }

    boolean startsAtBeginning() {
        return this.range == null || this.range.first == 0;
    }

    boolean isPartial() {
        return this.range != null;
    }

    ByteRange rangeWithin(long length) {
        return (this.range != null) ? this.range.within(length) : new ByteRange(0, length - 1);
    }

    private static boolean endsWithHeadEnd(byte[] head, int length) {
        if (length < HEAD_END.length) {
            return false;
        }
        for (int i = 0; i < HEAD_END.length; i++) {
            if (head[length - HEAD_END.length + i] != HEAD_END[i]) {
                return false;
            }
        }
        return true;
    }

    private static String findRange(String[] lines) {
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].regionMatches(true, 0, RANGE_HEADER, 0, RANGE_HEADER.length())) {
                return lines[i].substring(RANGE_HEADER.length()).trim();
            }
        }
        return null;
    }

    private static String validOrigin(String origin) {
        if (origin == null || !(origin.startsWith("http://") || origin.startsWith("https://"))) {
            return null;
        }
        for (int i = 0; i < origin.length(); i++) {
            char c = origin.charAt(i);
            if (c <= ' ' || c >= DEL) {
                return null;
            }
        }
        return origin;
    }

    private static long parseSize(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}

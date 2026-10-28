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
package hx.dash;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import hx.mp4.Mp4Boxes;
import hx.mp4.Segment;
import hx.util.RemoteFile;
import hx.util.Tasks;

final class SegmentProbe {

    private static final int PROBE_BYTES = 256;

    private static final int SIZING_THREADS = 48;

    private SegmentProbe() {
    }

    static void size(List<Segment> video, List<Segment> audio, CdnCookies cookies) throws IOException {
        List<Segment> all = new ArrayList<>(video);
        all.addAll(audio);
        ExecutorService pool = Executors.newFixedThreadPool(SIZING_THREADS);
        try {
            List<Future<Void>> probes = new ArrayList<>();
            for (Segment segment : all) {
                probes.add(pool.submit(() -> CdnClient.withRetries(() -> probe(segment, cookies))));
            }
            for (Future<Void> probe : probes) {
                Tasks.await(probe);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static int trackIdOffset(byte[] prefix, int moof, int end) throws IOException {
        if (moof + Mp4Boxes.HEADER_BYTES > end || !"moof".equals(Mp4Boxes.typeAt(prefix, moof))) {
            throw new IOException("segment does not start with moof");
        }
        int moofEnd = (int) Math.min((long) moof + Mp4Boxes.size(prefix, moof), end);
        int traf = moof + Mp4Boxes.HEADER_BYTES;
        while (traf + Mp4Boxes.HEADER_BYTES <= moofEnd) {
            int trafSize = Mp4Boxes.size(prefix, traf);
            if ("traf".equals(Mp4Boxes.typeAt(prefix, traf))) {
                int tfhd = findTfhd(prefix, traf, (int) Math.min((long) traf + trafSize, end));
                if (tfhd >= 0) {
                    return tfhd + Mp4Boxes.FULL_HEADER_BYTES;
                }
            }
            traf += trafSize;
        }
        throw new IOException("segment lacks a tfhd within " + end + " bytes");
    }

    private static int prefixLength(byte[] prefix, int available) throws IOException {
        int offset = 0;
        if (available >= Mp4Boxes.HEADER_BYTES && "styp".equals(Mp4Boxes.typeAt(prefix, 0))) {
            offset += Mp4Boxes.size(prefix, 0);
        }
        if (available >= offset + Mp4Boxes.HEADER_BYTES && "sidx".equals(Mp4Boxes.typeAt(prefix, offset))) {
            offset += Mp4Boxes.size(prefix, offset);
        }
        return offset;
    }

    private static Void probe(Segment segment, CdnCookies cookies) throws IOException {
        HttpURLConnection connection = CdnClient.connect(segment.url, "bytes=0-" + (PROBE_BYTES - 1), cookies, null);
        try (InputStream in = connection.getInputStream()) {
            segment.byteLength = totalLength(connection, segment.url);
            byte[] prefix = new byte[PROBE_BYTES];
            int got = readFully(in, prefix);
            segment.prefixBytes = prefixLength(prefix, got);
            segment.trackIdOffset = trackIdOffset(prefix, segment.prefixBytes, got);
        } catch (NumberFormatException ex) {
            throw new IOException("unreadable Content-Range for " + segment.url, ex);
        }
        return null;
    }

    private static long totalLength(HttpURLConnection connection, String url) throws IOException {
        String contentRange = connection.getHeaderField("Content-Range");
        boolean ranged = contentRange != null && contentRange.indexOf('/') >= 0;
        long length = ranged ? RemoteFile.totalFromContentRange(contentRange) : connection.getContentLength();
        if (length <= 0) {
            throw new IOException("no size for " + url);
        }
        return length;
    }

    private static int readFully(InputStream in, byte[] buffer) throws IOException {
        int got = 0;
        while (got < buffer.length) {
            int count = in.read(buffer, got, buffer.length - got);
            if (count < 0) {
                break;
            }
            got += count;
        }
        return got;
    }

    private static int findTfhd(byte[] prefix, int traf, int trafEnd) throws IOException {
        int tfhd = traf + Mp4Boxes.HEADER_BYTES;
        while (tfhd + Mp4Boxes.FULL_HEADER_BYTES + Mp4Boxes.U32_BYTES <= trafEnd) {
            if ("tfhd".equals(Mp4Boxes.typeAt(prefix, tfhd))) {
                return tfhd;
            }
            tfhd += Mp4Boxes.size(prefix, tfhd);
        }
        return -1;
    }

}

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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import hx.util.Tasks;

public final class CdnFile {

    public final long length;

    private final String url;

    private final CdnCookies cookies;

    private final long start;

    private final byte[] firstChunk;

    private CdnFile(String url, CdnCookies cookies, long start, byte[] firstChunk, long length) {
        this.url = url;
        this.cookies = cookies;
        this.start = start;
        this.firstChunk = firstChunk;
        this.length = length;
    }

    public static CdnFile open(String url, long start, CdnCookies cookies) throws IOException {
        long end = start + DashFile.PACED_READ_CHUNK_BYTES - 1;
        return CdnClient.withRetries(() -> {
            HttpURLConnection connection = CdnClient.connect(url, "bytes=" + start + "-" + end, cookies, null);
            try (InputStream in = connection.getInputStream()) {
                long length = totalLength(connection, url);
                byte[] firstChunk = new byte[(int) (Math.min(end, length - 1) - start + 1)];
                DashFile.readFully(in, firstChunk, url);
                return new CdnFile(url, cookies, start, firstChunk, length);
            }
        });
    }

    public void write(OutputStream out, long end) throws IOException {
        int firstCount = (int) Math.min(this.firstChunk.length, end - this.start + 1);
        out.write(this.firstChunk, 0, firstCount);
        long next = this.start + firstCount;
        if (next > end) {
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(DashFile.PACED_PARALLEL_READS);
        ReadGroup reads = new ReadGroup();
        ArrayDeque<Future<byte[]>> pending = new ArrayDeque<>();
        try {
            while (next <= end || !pending.isEmpty()) {
                while (pending.size() < DashFile.PACED_PARALLEL_READS && next <= end) {
                    long from = next;
                    long to = Math.min(end, from + DashFile.PACED_READ_CHUNK_BYTES - 1);
                    pending.add(pool.submit(() -> CdnClient.withRetries(() -> read(from, to, reads))));
                    next = to + 1;
                }
                out.write(Tasks.await(pending.poll()));
            }
        } finally {
            reads.close();
            pool.shutdownNow();
        }
    }

    private byte[] read(long from, long to, ReadGroup reads) throws IOException {
        HttpURLConnection connection = CdnClient.connect(this.url, "bytes=" + from + "-" + to, this.cookies, reads);
        try (InputStream in = connection.getInputStream()) {
            DashFile.requireContentRange(connection, this.url, from, to);
            byte[] bytes = new byte[(int) (to - from + 1)];
            DashFile.readFully(in, bytes, this.url);
            return bytes;
        } finally {
            reads.remove(connection);
        }
    }

    private static long totalLength(HttpURLConnection connection, String url) throws IOException {
        String contentRange = connection.getHeaderField("Content-Range");
        int slash = (contentRange != null) ? contentRange.lastIndexOf('/') : -1;
        if (connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL || slash < 0) {
            throw new IOException("no Content-Range length for " + url);
        }
        try {
            return Long.parseLong(contentRange.substring(slash + 1));
        } catch (NumberFormatException ex) {
            throw new IOException("unreadable Content-Range " + contentRange + " for " + url, ex);
        }
    }

}

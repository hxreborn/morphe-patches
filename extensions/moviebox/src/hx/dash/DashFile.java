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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.net.Uri;
import android.util.Log;
import hx.mp4.InterleavedTracks;
import hx.mp4.MovieHead;
import hx.mp4.Segment;
import hx.mp4.Track;
import hx.util.Logs;
import hx.util.Tasks;

public final class DashFile {

    private static final String UNPACED_CDN_HOST = "sacdn.hakunaymatata.com";

    private static final int UNPACED_PARALLEL_READS = 8;

    private static final int PACED_PARALLEL_READS = 16;

    private static final int UNPACED_READ_CHUNK_BYTES = 256 * 1024;

    private static final int PACED_READ_CHUNK_BYTES = 48 * 1024;

    private static final int READ_AHEAD_BYTES = 3 * 1024 * 1024;

    private static final int BYTES_PER_KIB = 1024;

    private static final double BYTES_PER_MB = 1e6;

    private static final double NANOS_PER_SECOND = 1e9;

    public final long length;

    private final CdnCookies cookies;

    private final byte[] head;

    private final Piece[] pieces;

    private final int readChunkBytes;

    private final int parallelReads;

    private DashFile(CdnCookies cookies, byte[] head, Piece[] pieces, boolean unpaced) {
        this.cookies = cookies;
        this.head = head;
        this.pieces = pieces;
        this.readChunkBytes = unpaced ? UNPACED_READ_CHUNK_BYTES : PACED_READ_CHUNK_BYTES;
        this.parallelReads = unpaced ? UNPACED_PARALLEL_READS : PACED_PARALLEL_READS;
        Piece last = pieces[pieces.length - 1];
        this.length = last.offset + last.length;
    }

    public static DashFile open(String manifestUrl, int height, CdnCookies cookies) throws IOException {
        byte[] xml = CdnClient.withRetries(() -> CdnClient.fetch(manifestUrl, cookies));
        DashManifest manifest = DashManifestParser.parse(xml, manifestUrl);
        DashManifest.Representation video = manifest.select("video", height);
        DashManifest.Representation audio = manifest.select("audio", Integer.MAX_VALUE);
        if (video == null || audio == null) {
            throw new IOException("manifest lacks a video or audio track");
        }
        List<Segment> videoSegments = manifest.segmentsOf(video);
        List<Segment> audioSegments = manifest.segmentsOf(audio);
        SegmentProbe.size(videoSegments, audioSegments, cookies);

        byte[] videoInit = fetchInitialization(video, cookies);
        byte[] audioInit = fetchInitialization(audio, cookies);
        int videoId = MovieHead.trackId(videoInit);
        Track videoTrack = new Track(videoInit, videoId, video.template.timescale, videoSegments);
        Track audioTrack = new Track(audioInit, videoId + 1, audio.template.timescale, audioSegments);

        InterleavedTracks ordered = InterleavedTracks.interleave(videoTrack, audioTrack);
        byte[] head = MovieHead.build(videoTrack, audioTrack, ordered);
        return new DashFile(cookies, head, layOut(head.length, ordered), isUnpacedCdn(manifestUrl));
    }

    public static boolean isUnpacedCdn(String manifestUrl) {
        return UNPACED_CDN_HOST.equals(Uri.parse(manifestUrl).getHost());
    }

    public String readPattern() {
        return this.parallelReads + " x " + this.readChunkBytes / BYTES_PER_KIB + " KiB reads";
    }

    public void write(OutputStream out, long start, long end) throws IOException {
        RangeTransfer transfer = new RangeTransfer(start, end);
        TransferLog.SHARED.rangeStarted();
        try {
            transfer.run(out);
        } finally {
            transfer.logEnd(TransferLog.SHARED.rangeEnded());
        }
    }

    private static byte[] fetchInitialization(DashManifest.Representation representation, CdnCookies cookies)
            throws IOException {
        return CdnClient.withRetries(() -> CdnClient.fetch(representation.initializationUrl(), cookies));
    }

    private static Piece[] layOut(long headBytes, InterleavedTracks ordered) {
        Piece[] pieces = new Piece[ordered.segments.size()];
        long offset = headBytes;
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new Piece(offset, ordered.segments.get(i), ordered.trackIds.get(i));
            offset += pieces[i].length;
        }
        return pieces;
    }

    private int pieceAt(long position) {
        int low = 0;
        int high = this.pieces.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (this.pieces[mid].offset + this.pieces[mid].length <= position) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private byte[] readPiece(Piece piece, long from, long to, ReadGroup reads) throws IOException {
        byte[] bytes = readRange(piece, piece.prefixBytes + from, piece.prefixBytes + to, reads);
        piece.patchTrackId(bytes, from);
        return bytes;
    }

    private byte[] readRange(Piece piece, long start, long end, ReadGroup reads) throws IOException {
        byte[] bytes = new byte[(int) (end - start + 1)];
        HttpURLConnection connection = null;
        long requested = System.nanoTime();
        long answered;
        try {
            connection = CdnClient.connect(piece.url, "bytes=" + start + "-" + end, this.cookies, reads);
            requireContentRange(connection, piece.url, start, end);
            answered = System.nanoTime();
            try (InputStream in = connection.getInputStream()) {
                readFully(in, bytes, piece.url);
            }
        } catch (IOException ex) {
            if (reads.isClosed()) {
                throw new InterruptedIOException("read cancelled: " + piece.url);
            }
            if (connection != null) {
                connection.disconnect();
            }
            throw ex;
        } finally {
            if (connection != null) {
                reads.remove(connection);
            }
        }
        TransferLog.SHARED.read(bytes.length, answered - requested, System.nanoTime() - answered);
        return bytes;
    }

    private static void requireContentRange(HttpURLConnection connection, String url, long start, long end)
            throws IOException {
        String contentRange = connection.getHeaderField("Content-Range");
        if (connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL || contentRange == null
                || !contentRange.startsWith("bytes " + start + "-" + end + "/")) {
            throw new IOException("expected bytes " + start + "-" + end + ", got " + contentRange + " for " + url);
        }
    }

    private static void readFully(InputStream in, byte[] bytes, String url) throws IOException {
        for (int got = 0; got < bytes.length;) {
            int count = in.read(bytes, got, bytes.length - got);
            if (count < 0) {
                throw new IOException("segment ended early: " + url);
            }
            got += count;
        }
    }

    private final class RangeTransfer {

        private final long start;

        private final long end;

        private final long begun = System.nanoTime();

        private long served;

        private long at;

        private int index;

        private final ArrayDeque<Future<byte[]>> pending = new ArrayDeque<>();

        private ExecutorService pool;

        private ReadGroup reads;

        RangeTransfer(long start, long end) {
            this.start = start;
            this.end = end;
        }

        void run(OutputStream out) throws IOException {
            this.at = this.start;
            writeHead(out);
            out.flush();
            this.pool = Executors.newFixedThreadPool(DashFile.this.parallelReads);
            this.reads = new ReadGroup();
            try {
                this.index = pieceAt(this.at);
                streamPieces(out);
            } finally {
                this.reads.close();
                this.pool.shutdownNow();
            }
        }

        void logEnd(int rangesStillOpen) {
            double seconds = (System.nanoTime() - this.begun) / NANOS_PER_SECOND;
            Log.i(Logs.TAG,
                    String.format(Locale.ROOT, "DASH range %d-%d: %.1f MB in %.1f s = %.1f MB/s, %d ranges still open",
                            this.start, this.end, this.served / BYTES_PER_MB, seconds,
                            this.served / BYTES_PER_MB / seconds, rangesStillOpen));
        }

        private void writeHead(OutputStream out) throws IOException {
            if (this.at < DashFile.this.head.length) {
                int count = (int) (Math.min(this.end + 1, DashFile.this.head.length) - this.at);
                out.write(DashFile.this.head, (int) this.at, count);
                this.at += count;
                this.served += count;
            }
        }

        private void streamPieces(OutputStream out) throws IOException {
            int readAheadChunks = Math.max(DashFile.this.parallelReads,
                    READ_AHEAD_BYTES / DashFile.this.readChunkBytes);
            while (true) {
                scheduleReads(readAheadChunks);
                Future<byte[]> next = this.pending.poll();
                if (next == null) {
                    return;
                }
                byte[] chunk = Tasks.await(next);
                out.write(chunk);
                this.served += chunk.length;
                TransferLog.SHARED.served(chunk.length);
            }
        }

        private void scheduleReads(int readAheadChunks) {
            while (this.pending.size() < readAheadChunks && this.at <= this.end
                    && this.index < DashFile.this.pieces.length) {
                Piece piece = DashFile.this.pieces[this.index];
                long from = this.at - piece.offset;
                long to = Math.min(Math.min(this.end - piece.offset, piece.length - 1),
                        from + DashFile.this.readChunkBytes - 1);
                ReadGroup group = this.reads;
                this.pending.add(this.pool
                    .submit(() -> CdnClient.withRetries(() -> DashFile.this.readPiece(piece, from, to, group))));
                this.at = piece.offset + to + 1;
                if (to == piece.length - 1) {
                    this.index++;
                }
            }
        }

    }

}

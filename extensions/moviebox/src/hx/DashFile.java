/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class DashFile {
    interface CookieProvider {
        String current() throws IOException;

        String refresh(String rejected) throws IOException;
    }

    private static final String TAG = "hxreborn/moviebox";
    private static final int PROBE_BYTES = 256;
    private static final int SIZING_THREADS = 48;
    private static final int UNPACED_PARALLEL_READS = 8;
    private static final int PACED_PARALLEL_READS = 16;
    private static final String UNPACED_CDN_HOST = "sacdn.hakunaymatata.com";
    private static final int UNPACED_READ_CHUNK_BYTES = 256 * 1024;
    private static final int PACED_READ_CHUNK_BYTES = 48 * 1024;
    private static final int READ_AHEAD_BYTES = 3 * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int CDN_ATTEMPTS = 5;
    private static final long CDN_RETRY_DELAY_MS = 2000L;
    private static final int COPY_BUFFER_BYTES = 1 << 16;
    private static final int MAX_MANIFEST_DEPTH = 32;
    private static final long MS_PER_SECOND = 1000L;
    private static final String REPRESENTATION_ID = "$RepresentationID$";
    private static final Pattern NUMBER = Pattern.compile("\\$Number(?:%0(\\d+)d)?\\$");
    private static final Pattern ISO_DURATION =
            Pattern.compile("P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?");
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 3600L;
    private static final long SECONDS_PER_DAY = 86400L;

    private static final int BOX_HEADER = 8;
    private static final int BOX_TYPE_AT = 4;
    private static final int FULL_BOX_HEADER = 12;
    private static final int VERSION_AT = 8;
    private static final int V0_TIMESCALE = 20;
    private static final int V1_TIMESCALE = 28;
    private static final int V0_DURATION = 24;
    private static final int V1_DURATION = 32;
    private static final int TKHD_V0_TRACK_ID = 20;
    private static final int TKHD_V1_TRACK_ID = 28;
    private static final int TKHD_V0_DURATION = 28;
    private static final int TKHD_V1_DURATION = 36;
    private static final int U32_BYTES = 4;
    private static final int TRACK_ID_BYTES = U32_BYTES;
    private static final int SIDX_FIXED_BYTES = 24;
    private static final int SIDX_REFERENCE_ID = 4;
    private static final int SIDX_TIMESCALE = 8;
    private static final int SIDX_EARLIEST_TIME = 12;
    private static final int SIDX_REFERENCE_COUNT = 22;
    private static final int SIDX_REFERENCE_BYTES = 12;
    private static final int REFERENCE_SIZE_AT = 0;
    private static final int REFERENCE_DURATION_AT = 4;
    private static final int REFERENCE_SAP_AT = 8;
    private static final int SAP_FLAGS = 0x90000000;

    private static final class Box {
        final int offset;
        final int size;

        Box(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }

        int end() {
            return offset + size;
        }
    }

    private static final class Interval {
        final long startTime;
        final long duration;
        final long repeats;

        Interval(long startTime, long duration, long repeats) {
            this.startTime = startTime;
            this.duration = duration;
            this.repeats = repeats;
        }
    }

    private static final class Template {
        long timescale = 1;
        long duration;
        String initialization;
        String media;
        int startNumber = 1;
        final List<Interval> timeline = new ArrayList<>();
    }

    private static final class Manifest {
        final List<Representation> representations = new ArrayList<>();
        double presentationSeconds;
    }

    private static final class Representation {
        String id;
        String type;
        int height;
        int bandwidth;
        String baseUrl;
        Template template;
    }

    private static final class Segment {
        String url;
        long startTime;
        long duration;
        long byteLength;
        int prefixBytes;
        int trackIdOffset;
    }

    private static final class Reference {
        final long byteLength;
        final long duration;

        Reference(long byteLength, long duration) {
            this.byteLength = byteLength;
            this.duration = duration;
        }
    }

    private static final class ReadGroup {
        private final Set<HttpURLConnection> open = new HashSet<>();
        private boolean closed;

        synchronized void add(HttpURLConnection connection) throws InterruptedIOException {
            if (closed) {
                connection.disconnect();
                throw new InterruptedIOException("read cancelled");
            }
            open.add(connection);
        }

        synchronized void remove(HttpURLConnection connection) {
            open.remove(connection);
        }

        synchronized boolean isClosed() {
            return closed;
        }

        synchronized void close() {
            closed = true;
            for (HttpURLConnection connection : open) connection.disconnect();
            open.clear();
        }
    }

    private static final class Piece {
        final long offset;
        final long length;
        final String url;
        final int prefixBytes;
        final int trackIdOffset;
        final int trackId;

        Piece(long offset, Segment segment, int trackId) {
            this.offset = offset;
            this.length = segment.byteLength - segment.prefixBytes;
            this.url = segment.url;
            this.prefixBytes = segment.prefixBytes;
            this.trackIdOffset = segment.trackIdOffset - segment.prefixBytes;
            this.trackId = trackId;
        }
    }

    private final CookieProvider cookies;
    private final byte[] head;
    private final Piece[] pieces;
    private final int readChunkBytes;
    private final int parallelReads;
    final long length;

    private DashFile(CookieProvider cookies, byte[] head, Piece[] pieces, boolean unpaced) {
        this.cookies = cookies;
        this.head = head;
        this.pieces = pieces;
        this.readChunkBytes = unpaced ? UNPACED_READ_CHUNK_BYTES : PACED_READ_CHUNK_BYTES;
        this.parallelReads = unpaced ? UNPACED_PARALLEL_READS : PACED_PARALLEL_READS;
        Piece last = pieces[pieces.length - 1];
        this.length = last.offset + last.length;
    }

    static DashFile open(String manifestUrl, int height, CookieProvider cookies) throws IOException {
        Manifest manifest = parse(withRetries(() -> fetch(manifestUrl, cookies)), manifestUrl);
        Representation video = select(manifest.representations, "video", height);
        Representation audio = select(manifest.representations, "audio", Integer.MAX_VALUE);
        if (video == null || audio == null) throw new IOException("manifest lacks a video or audio track");

        List<Segment> videoSegments = buildSegments(video, manifest.presentationSeconds);
        List<Segment> audioSegments = buildSegments(audio, manifest.presentationSeconds);
        size(videoSegments, audioSegments, cookies);

        byte[] videoInit = withRetries(() -> fetch(initializationUrl(video), cookies));
        byte[] audioInit = withRetries(() -> fetch(initializationUrl(audio), cookies));
        int videoId = trackId(videoInit);
        int audioId = videoId + 1;
        long templateTimescale = video.template.timescale;
        long videoTimescale = mediaTimescale(videoInit);
        long durationUnits = 0;
        for (Segment segment : videoSegments) durationUnits += segment.duration;
        long durationMs = durationUnits * MS_PER_SECOND / templateTimescale;

        List<Segment> ordered = new ArrayList<>();
        List<Integer> trackIds = new ArrayList<>();
        List<Reference> references = new ArrayList<>();
        int nextAudio = 0;
        double audioScale = (double) templateTimescale / audio.template.timescale;
        long firstStart = videoSegments.get(0).startTime;
        long elapsed = firstStart;
        long indexedEnd = firstStart * videoTimescale / templateTimescale;
        for (int i = 0; i < videoSegments.size(); i++) {
            Segment videoSegment = videoSegments.get(i);
            long groupBytes = videoSegment.byteLength - videoSegment.prefixBytes;
            ordered.add(videoSegment);
            trackIds.add(videoId);
            boolean last = i + 1 == videoSegments.size();
            long until = last ? Long.MAX_VALUE : videoSegments.get(i + 1).startTime;
            while (nextAudio < audioSegments.size()
                    && (last || audioSegments.get(nextAudio).startTime * audioScale < until)) {
                Segment audioSegment = audioSegments.get(nextAudio++);
                ordered.add(audioSegment);
                trackIds.add(audioId);
                groupBytes += audioSegment.byteLength - audioSegment.prefixBytes;
            }
            elapsed += videoSegment.duration;
            long end = elapsed * videoTimescale / templateTimescale;
            references.add(new Reference(groupBytes, end - indexedEnd));
            indexedEnd = end;
        }

        byte[] moov = moov(videoInit, audioInit, audioId, durationMs);
        byte[] sidx = sidx(videoId, videoTimescale, firstStart * videoTimescale / templateTimescale, references);
        byte[] head = concat(slice(videoInit, child(videoInit, 0, videoInit.length, "ftyp")), moov, sidx);
        Piece[] pieces = new Piece[ordered.size()];
        long offset = head.length;
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new Piece(offset, ordered.get(i), trackIds.get(i));
            offset += pieces[i].length;
        }
        return new DashFile(cookies, head, pieces, isUnpacedCdn(manifestUrl));
    }

    static boolean isUnpacedCdn(String manifestUrl) {
        return UNPACED_CDN_HOST.equals(Uri.parse(manifestUrl).getHost());
    }

    void write(OutputStream out, long start, long end) throws IOException {
        long at = start;
        if (at < head.length) {
            int count = (int) (Math.min(end + 1, head.length) - at);
            out.write(head, (int) at, count);
            at += count;
        }
        out.flush();
        ExecutorService pool = Executors.newFixedThreadPool(parallelReads);
        ReadGroup reads = new ReadGroup();
        ArrayDeque<Future<byte[]>> pending = new ArrayDeque<>();
        int readAheadChunks = Math.max(parallelReads, READ_AHEAD_BYTES / readChunkBytes);
        try {
            int index = pieceAt(at);
            while (true) {
                while (pending.size() < readAheadChunks && at <= end && index < pieces.length) {
                    final Piece piece = pieces[index];
                    final long from = at - piece.offset;
                    final long to = Math.min(Math.min(end - piece.offset, piece.length - 1), from + readChunkBytes - 1);
                    pending.add(pool.submit(new Callable<byte[]>() {
                        @Override
                        public byte[] call() throws IOException {
                            return withRetries(() -> readPiece(piece, from, to, reads));
                        }
                    }));
                    at = piece.offset + to + 1;
                    if (to == piece.length - 1) index++;
                }
                Future<byte[]> next = pending.poll();
                if (next == null) return;
                out.write(await(next));
            }
        } finally {
            reads.close();
            pool.shutdownNow();
        }
    }

    private static <T> T await(Future<T> task) throws IOException {
        try {
            return task.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private int pieceAt(long position) {
        int low = 0;
        int high = pieces.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (pieces[mid].offset + pieces[mid].length <= position) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    private interface CdnRequest<T> {
        T send() throws IOException;
    }

    private static <T> T withRetries(CdnRequest<T> request) throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                return request.send();
            } catch (IOException failure) {
                boolean cancelled = failure instanceof InterruptedIOException
                        && !(failure instanceof SocketTimeoutException);
                if (attempt == CDN_ATTEMPTS || cancelled) throw failure;
                Log.w(TAG, "CDN read attempt " + attempt + " of " + CDN_ATTEMPTS + " failed", failure);
                try {
                    Thread.sleep(CDN_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
        }
    }

    private byte[] readPiece(Piece piece, long from, long to, ReadGroup reads) throws IOException {
        long start = piece.prefixBytes + from;
        long end = piece.prefixBytes + to;
        byte[] bytes = new byte[(int) (to - from + 1)];
        HttpURLConnection connection = null;
        try {
            connection = connect(piece.url, "bytes=" + start + "-" + end, cookies, reads);
            String contentRange = connection.getHeaderField("Content-Range");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL
                    || contentRange == null || !contentRange.startsWith("bytes " + start + "-" + end + "/")) {
                throw new IOException("expected bytes " + start + "-" + end + ", got " + contentRange + " for " + piece.url);
            }
            try (InputStream in = connection.getInputStream()) {
                for (int got = 0; got < bytes.length; ) {
                    int count = in.read(bytes, got, bytes.length - got);
                    if (count < 0) throw new IOException("segment ended early: " + piece.url);
                    got += count;
                }
            }
        } catch (IOException e) {
            if (reads.isClosed()) throw new InterruptedIOException("read cancelled: " + piece.url);
            if (connection != null) connection.disconnect();
            throw e;
        } finally {
            if (connection != null) reads.remove(connection);
        }
        for (int k = 0; k < TRACK_ID_BYTES; k++) {
            long index = piece.trackIdOffset + k - from;
            if (index >= 0 && index < bytes.length) {
                bytes[(int) index] = (byte) (piece.trackId >>> (Byte.SIZE * (TRACK_ID_BYTES - 1 - k)));
            }
        }
        return bytes;
    }

    private static Manifest parse(byte[] xml, String manifestUrl) throws IOException {
        Manifest manifest = new Manifest();
        List<Representation> representations = manifest.representations;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(xml), null);
            String[] baseUrls = new String[MAX_MANIFEST_DEPTH];
            baseUrls[0] = manifestUrl.substring(0, manifestUrl.lastIndexOf('/') + 1);
            String setType = null;
            Template setTemplate = null;
            Template template = null;
            Representation representation = null;
            for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
                int depth = parser.getDepth();
                if (depth >= MAX_MANIFEST_DEPTH) {
                    throw new IOException("manifest nested deeper than " + MAX_MANIFEST_DEPTH);
                }
                if (event == XmlPullParser.START_TAG) {
                    baseUrls[depth] = baseUrls[depth - 1];
                    String name = parser.getName();
                    if ("MPD".equals(name)) {
                        manifest.presentationSeconds =
                                parseDurationSeconds(parser.getAttributeValue(null, "mediaPresentationDuration"));
                    } else if ("BaseURL".equals(name)) {
                        URL base = new URL(baseUrls[depth - 1]);
                        baseUrls[depth - 1] = new URL(base, parser.nextText().trim()).toString();
                        baseUrls[depth] = baseUrls[depth - 1];
                    } else if ("AdaptationSet".equals(name)) {
                        setType = readContentType(parser);
                        setTemplate = null;
                    } else if ("Representation".equals(name)) {
                        representation = new Representation();
                        representation.id = parser.getAttributeValue(null, "id");
                        representation.height = parseIntAttribute(parser, "height", 0);
                        representation.bandwidth = parseIntAttribute(parser, "bandwidth", 0);
                        String ownType = readContentType(parser);
                        representation.type = ownType != null ? ownType : representation.height > 0 ? "video" : setType;
                    } else if ("SegmentTemplate".equals(name)) {
                        template = new Template();
                        template.timescale = parseIntAttribute(parser, "timescale", 1);
                        template.duration = parseLongAttribute(parser, "duration", 0);
                        template.initialization = parser.getAttributeValue(null, "initialization");
                        template.media = parser.getAttributeValue(null, "media");
                        template.startNumber = parseIntAttribute(parser, "startNumber", 1);
                        if (representation == null) setTemplate = template;
                        else representation.template = template;
                    } else if ("S".equals(name) && template != null) {
                        template.timeline.add(new Interval(parseLongAttribute(parser, "t", -1),
                                parseLongAttribute(parser, "d", 0), parseLongAttribute(parser, "r", 0)));
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    if ("Representation".equals(parser.getName()) && representation != null) {
                        if (representation.template == null) representation.template = setTemplate;
                        representation.baseUrl = baseUrls[depth];
                        if (representation.template != null && representation.template.media != null) {
                            representations.add(representation);
                        }
                        representation = null;
                    } else if ("SegmentTemplate".equals(parser.getName())) {
                        template = null;
                    }
                }
            }
        } catch (XmlPullParserException | NumberFormatException | MalformedURLException malformed) {
            throw new IOException("cannot parse manifest " + manifestUrl, malformed);
        }
        return manifest;
    }

    private static double parseDurationSeconds(String iso8601Duration) {
        if (iso8601Duration == null) return 0;
        Matcher matcher = ISO_DURATION.matcher(iso8601Duration);
        if (!matcher.matches()) throw new NumberFormatException("duration " + iso8601Duration);
        double seconds = 0;
        if (matcher.group(1) != null) seconds += Long.parseLong(matcher.group(1)) * SECONDS_PER_DAY;
        if (matcher.group(2) != null) seconds += Long.parseLong(matcher.group(2)) * SECONDS_PER_HOUR;
        if (matcher.group(3) != null) seconds += Long.parseLong(matcher.group(3)) * SECONDS_PER_MINUTE;
        if (matcher.group(4) != null) seconds += Double.parseDouble(matcher.group(4));
        return seconds;
    }

    private static String readContentType(XmlPullParser parser) {
        String type = parser.getAttributeValue(null, "contentType");
        if (type != null) return type;
        String mime = parser.getAttributeValue(null, "mimeType");
        return mime == null ? null : mime.substring(0, Math.max(mime.indexOf('/'), 0));
    }

    private static Representation select(List<Representation> representations, String type, int maxHeight) {
        Representation best = null;
        Representation smallest = null;
        for (Representation candidate : representations) {
            if (!type.equals(candidate.type)) continue;
            if (smallest == null || candidate.height < smallest.height) smallest = candidate;
            boolean better = best == null || candidate.height > best.height
                    || (candidate.height == best.height && candidate.bandwidth > best.bandwidth);
            if (candidate.height <= maxHeight && better) best = candidate;
        }
        return best != null ? best : smallest;
    }

    private static String initializationUrl(Representation representation) throws IOException {
        String initialization = representation.template.initialization;
        if (initialization == null) {
            throw new IOException("representation " + representation.id + " has no initialization");
        }
        return representation.baseUrl + initialization.replace(REPRESENTATION_ID, representation.id);
    }

    private static List<Segment> buildSegments(Representation representation, double presentationSeconds)
            throws IOException {
        Template template = representation.template;
        if (template.timescale <= 0) throw new IOException("representation " + representation.id + " has no timescale");
        List<Interval> timeline = template.timeline;
        if (timeline.isEmpty() && template.duration > 0 && presentationSeconds > 0) {
            long total = Math.round(presentationSeconds * template.timescale);
            long fullSegments = (total - 1) / template.duration;
            timeline = new ArrayList<>();
            if (fullSegments > 0) timeline.add(new Interval(0, template.duration, fullSegments - 1));
            timeline.add(new Interval(fullSegments * template.duration, total - fullSegments * template.duration, 0));
        }
        if (timeline.isEmpty()) throw new IOException("manifest lacks a segment timeline or segment duration");
        List<Segment> segments = new ArrayList<>();
        long time = 0;
        int number = template.startNumber;
        for (Interval interval : timeline) {
            if (interval.duration <= 0 || interval.repeats < 0) {
                throw new IOException("representation " + representation.id + " has an invalid segment timeline");
            }
            if (interval.startTime >= 0) time = interval.startTime;
            for (long repeat = 0; repeat <= interval.repeats; repeat++) {
                Segment segment = new Segment();
                segment.url = representation.baseUrl + resolveMediaUrl(template.media, representation.id, number++);
                segment.startTime = time;
                segment.duration = interval.duration;
                time += interval.duration;
                segments.add(segment);
            }
        }
        return segments;
    }

    private static String resolveMediaUrl(String media, String representationId, int number) {
        Matcher matcher = NUMBER.matcher(media.replace(REPRESENTATION_ID, representationId));
        StringBuffer url = new StringBuffer();
        while (matcher.find()) {
            String width = matcher.group(1);
            String value = width == null ? Integer.toString(number)
                    : String.format(Locale.US, "%0" + width + "d", number);
            matcher.appendReplacement(url, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(url);
        return url.toString();
    }

    private static void size(List<Segment> video, List<Segment> audio, final CookieProvider cookies) throws IOException {
        List<Segment> all = new ArrayList<>(video);
        all.addAll(audio);
        ExecutorService pool = Executors.newFixedThreadPool(SIZING_THREADS);
        try {
            List<Future<Void>> probes = new ArrayList<>();
            for (final Segment segment : all) {
                probes.add(pool.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws IOException {
                        return withRetries(() -> {
                            probe(segment, cookies);
                            return null;
                        });
                    }
                }));
            }
            for (Future<Void> probe : probes) await(probe);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void probe(Segment segment, CookieProvider cookies) throws IOException {
        HttpURLConnection connection = connect(segment.url, "bytes=0-" + (PROBE_BYTES - 1), cookies, null);
        try (InputStream in = connection.getInputStream()) {
            String contentRange = connection.getHeaderField("Content-Range");
            int slash = contentRange == null ? -1 : contentRange.indexOf('/');
            segment.byteLength = slash >= 0
                    ? Long.parseLong(contentRange.substring(slash + 1).trim())
                    : connection.getContentLength();
            if (segment.byteLength <= 0) throw new IOException("no size for " + segment.url);
            byte[] prefix = new byte[PROBE_BYTES];
            int got = 0;
            while (got < prefix.length) {
                int count = in.read(prefix, got, prefix.length - got);
                if (count < 0) break;
                got += count;
            }
            int offset = 0;
            if (got >= BOX_HEADER && "styp".equals(readBoxType(prefix, 0))) offset += boxSize(prefix, 0);
            if (got >= offset + BOX_HEADER && "sidx".equals(readBoxType(prefix, offset))) offset += boxSize(prefix, offset);
            segment.prefixBytes = offset;
            segment.trackIdOffset = trackIdOffset(prefix, offset, got);
        } catch (NumberFormatException malformed) {
            throw new IOException("unreadable Content-Range for " + segment.url, malformed);
        }
    }

    private static int trackIdOffset(byte[] prefix, int moof, int end) throws IOException {
        if (moof + BOX_HEADER > end || !"moof".equals(readBoxType(prefix, moof))) {
            throw new IOException("segment does not start with moof");
        }
        int moofEnd = (int) Math.min((long) moof + boxSize(prefix, moof), end);
        int traf = moof + BOX_HEADER;
        while (traf + BOX_HEADER <= moofEnd) {
            int trafSize = boxSize(prefix, traf);
            if ("traf".equals(readBoxType(prefix, traf))) {
                int trafEnd = (int) Math.min((long) traf + trafSize, end);
                int tfhd = traf + BOX_HEADER;
                while (tfhd + FULL_BOX_HEADER + TRACK_ID_BYTES <= trafEnd) {
                    if ("tfhd".equals(readBoxType(prefix, tfhd))) return tfhd + FULL_BOX_HEADER;
                    tfhd += boxSize(prefix, tfhd);
                }
            }
            traf += trafSize;
        }
        throw new IOException("segment lacks a tfhd within " + end + " bytes");
    }

    private static byte[] fetch(String url, CookieProvider cookies) throws IOException {
        try (InputStream in = connect(url, null, cookies, null).getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[COPY_BUFFER_BYTES];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toByteArray();
        }
    }

    private static HttpURLConnection connect(String url, String range, CookieProvider cookies, ReadGroup reads)
            throws IOException {
        String cookie = cookies.current();
        HttpURLConnection connection = request(url, range, cookie, reads);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
            connection.disconnect();
            if (reads != null) reads.remove(connection);
            connection = request(url, range, cookies.refresh(cookie), reads);
        }
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            connection.disconnect();
            throw new IOException("HTTP " + code + " for " + url);
        }
        return connection;
    }

    private static HttpURLConnection request(String url, String range, String cookie, ReadGroup reads)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Cookie", cookie);
        if (range != null) connection.setRequestProperty("Range", range);
        if (reads != null) reads.add(connection);
        return connection;
    }

    private static byte[] moov(byte[] videoInit, byte[] audioInit, int audioId, long durationMs) throws IOException {
        Box videoMoov = child(videoInit, 0, videoInit.length, "moov");
        Box audioMoov = child(audioInit, 0, audioInit.length, "moov");
        byte[] mvhd = slice(videoInit, child(videoInit, videoMoov.offset + BOX_HEADER, videoMoov.end(), "mvhd"));
        int version = mvhd[VERSION_AT];
        long movieTimescale = u32(mvhd, version == 1 ? V1_TIMESCALE : V0_TIMESCALE);
        putDuration(mvhd, version == 1 ? V1_DURATION : V0_DURATION, version,
                durationMs * movieTimescale / MS_PER_SECOND);
        putU32(mvhd, mvhd.length - U32_BYTES, audioId + 1);

        byte[] videoTrak = withDurations(slice(videoInit, trak(videoInit)), movieTimescale, durationMs);
        byte[] audioTrak = withDurations(slice(audioInit, trak(audioInit)), movieTimescale, durationMs);
        Box tkhd = child(audioTrak, BOX_HEADER, audioTrak.length, "tkhd");
        int trackIdAt = audioTrak[tkhd.offset + VERSION_AT] == 1 ? TKHD_V1_TRACK_ID : TKHD_V0_TRACK_ID;
        putU32(audioTrak, tkhd.offset + trackIdAt, audioId);

        byte[] videoTrex = trex(videoInit, videoMoov);
        byte[] audioTrex = trex(audioInit, audioMoov);
        putU32(audioTrex, FULL_BOX_HEADER, audioId);
        return box("moov", concat(mvhd, videoTrak, audioTrak, box("mvex", concat(videoTrex, audioTrex))));
    }

    private static Box trak(byte[] init) throws IOException {
        Box moov = child(init, 0, init.length, "moov");
        return child(init, moov.offset + BOX_HEADER, moov.end(), "trak");
    }

    private static byte[] trex(byte[] init, Box moov) throws IOException {
        Box mvex = child(init, moov.offset + BOX_HEADER, moov.end(), "mvex");
        return slice(init, child(init, mvex.offset + BOX_HEADER, mvex.end(), "trex"));
    }

    private static long mediaTimescale(byte[] init) throws IOException {
        Box trak = trak(init);
        Box mdia = child(init, trak.offset + BOX_HEADER, trak.end(), "mdia");
        Box mdhd = child(init, mdia.offset + BOX_HEADER, mdia.end(), "mdhd");
        return u32(init, mdhd.offset + (init[mdhd.offset + VERSION_AT] == 1 ? V1_TIMESCALE : V0_TIMESCALE));
    }

    private static int trackId(byte[] init) throws IOException {
        Box trak = trak(init);
        Box tkhd = child(init, trak.offset + BOX_HEADER, trak.end(), "tkhd");
        int trackIdAt = init[tkhd.offset + VERSION_AT] == 1 ? TKHD_V1_TRACK_ID : TKHD_V0_TRACK_ID;
        return (int) u32(init, tkhd.offset + trackIdAt);
    }

    private static byte[] withDurations(byte[] trak, long movieTimescale, long durationMs) throws IOException {
        Box tkhd = child(trak, BOX_HEADER, trak.length, "tkhd");
        int version = trak[tkhd.offset + VERSION_AT];
        putDuration(trak, tkhd.offset + (version == 1 ? TKHD_V1_DURATION : TKHD_V0_DURATION), version,
                durationMs * movieTimescale / MS_PER_SECOND);
        Box mdia = child(trak, BOX_HEADER, trak.length, "mdia");
        Box mdhd = child(trak, mdia.offset + BOX_HEADER, mdia.end(), "mdhd");
        version = trak[mdhd.offset + VERSION_AT];
        long mediaTimescale = u32(trak, mdhd.offset + (version == 1 ? V1_TIMESCALE : V0_TIMESCALE));
        putDuration(trak, mdhd.offset + (version == 1 ? V1_DURATION : V0_DURATION), version,
                durationMs * mediaTimescale / MS_PER_SECOND);
        return trak;
    }

    private static byte[] sidx(int referenceId, long timescale, long earliestTime, List<Reference> references) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] fixed = new byte[SIDX_FIXED_BYTES];
        putU32(fixed, SIDX_REFERENCE_ID, referenceId);
        putU32(fixed, SIDX_TIMESCALE, timescale);
        putU32(fixed, SIDX_EARLIEST_TIME, earliestTime);
        putU16(fixed, SIDX_REFERENCE_COUNT, references.size());
        out.write(fixed, 0, fixed.length);
        byte[] entry = new byte[SIDX_REFERENCE_BYTES];
        for (Reference reference : references) {
            putU32(entry, REFERENCE_SIZE_AT, reference.byteLength);
            putU32(entry, REFERENCE_DURATION_AT, reference.duration);
            putU32(entry, REFERENCE_SAP_AT, SAP_FLAGS);
            out.write(entry, 0, entry.length);
        }
        return box("sidx", out.toByteArray());
    }

    private static Box child(byte[] data, int from, int end, String type) throws IOException {
        int at = from;
        while (at + BOX_HEADER <= end) {
            int size = boxSize(data, at);
            if ((long) at + size > end) throw new IOException("box " + readBoxType(data, at) + " overruns its parent");
            if (type.equals(readBoxType(data, at))) return new Box(at, size);
            at += size;
        }
        throw new IOException("missing " + type + " box");
    }

    private static int boxSize(byte[] data, int at) throws IOException {
        long size = u32(data, at);
        if (size < BOX_HEADER || size > Integer.MAX_VALUE) throw new IOException("unsupported box size " + size);
        return (int) size;
    }

    private static String readBoxType(byte[] data, int at) {
        return new String(data, at + BOX_TYPE_AT, BOX_TYPE_AT, StandardCharsets.ISO_8859_1);
    }

    private static long u32(byte[] data, int at) {
        return ((data[at] & 0xFFL) << 24) | ((data[at + 1] & 0xFFL) << 16)
                | ((data[at + 2] & 0xFFL) << 8) | (data[at + 3] & 0xFFL);
    }

    private static void putU32(byte[] data, int at, long value) {
        data[at] = (byte) (value >>> 24);
        data[at + 1] = (byte) (value >>> 16);
        data[at + 2] = (byte) (value >>> 8);
        data[at + 3] = (byte) value;
    }

    private static void putU16(byte[] data, int at, int value) {
        data[at] = (byte) (value >>> 8);
        data[at + 1] = (byte) value;
    }

    private static void putDuration(byte[] data, int at, int version, long value) {
        if (version == 1) {
            putU32(data, at, value >>> Integer.SIZE);
            putU32(data, at + U32_BYTES, value);
        } else {
            putU32(data, at, value);
        }
    }

    private static byte[] slice(byte[] data, Box box) {
        byte[] out = new byte[box.size];
        System.arraycopy(data, box.offset, out, 0, box.size);
        return out;
    }

    private static byte[] box(String type, byte[] payload) {
        byte[] out = new byte[BOX_HEADER + payload.length];
        putU32(out, 0, out.length);
        System.arraycopy(type.getBytes(StandardCharsets.ISO_8859_1), 0, out, BOX_TYPE_AT, BOX_TYPE_AT);
        System.arraycopy(payload, 0, out, BOX_HEADER, payload.length);
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] part : parts) total += part.length;
        byte[] out = new byte[total];
        int at = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, at, part.length);
            at += part.length;
        }
        return out;
    }

    private static int parseIntAttribute(XmlPullParser parser, String name, int fallback) {
        String value = parser.getAttributeValue(null, name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static long parseLongAttribute(XmlPullParser parser, String name, long fallback) {
        String value = parser.getAttributeValue(null, name);
        return value == null ? fallback : Long.parseLong(value);
    }
}

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
package hx.mp4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MovieHead {

    private static final int V0_TIMESCALE = 20;

    private static final int V1_TIMESCALE = 28;

    private static final int V0_DURATION = 24;

    private static final int V1_DURATION = 32;

    private static final int TKHD_V0_TRACK_ID = 20;

    private static final int TKHD_V1_TRACK_ID = 28;

    private static final int TKHD_V0_DURATION = 28;

    private static final int TKHD_V1_DURATION = 36;

    private static final int SIDX_FIXED_BYTES = 24;

    private static final int SIDX_REFERENCE_ID = 4;

    private static final int SIDX_TIMESCALE = 8;

    private static final int SIDX_EARLIEST_TIME = 12;

    private static final int SIDX_FIRST_OFFSET = 16;

    private static final int SIDX_REFERENCE_COUNT = 22;

    private static final int SIDX_REFERENCE_BYTES = 12;

    private static final int REFERENCE_SIZE_AT = 0;

    private static final int REFERENCE_DURATION_AT = 4;

    private static final int REFERENCE_SAP_AT = 8;

    private static final int SAP_FLAGS = 0x90000000;

    private static final long MILLIS_PER_SECOND = 1000L;

    private MovieHead() {
    }

    public static byte[] build(Track video, Track audio, InterleavedTracks ordered) throws IOException {
        byte[] moov = moov(video.init, audio.init, audio.id, video.durationMillis());
        byte[] audioIndex = sidxBoxFor(audio, ordered, 0);
        byte[] videoIndex = sidxBoxFor(video, ordered, audioIndex.length);
        byte[] ftyp = Mp4Boxes.slice(video.init, Mp4Boxes.child(video.init, 0, video.init.length, "ftyp"));
        return Mp4Boxes.concat(ftyp, moov, videoIndex, audioIndex);
    }

    public static int trackId(byte[] init) throws IOException {
        Mp4Boxes.Box tkhd = Mp4Boxes.childOf(init, trak(init), "tkhd");
        return (int) Mp4Boxes.u32(init, tkhd.offset + trackIdOffset(init, tkhd.offset));
    }

    static long mediaTimescale(byte[] init) throws IOException {
        Mp4Boxes.Box mdhd = mdhd(init, trak(init));
        return Mp4Boxes.u32(init, mdhd.offset + timescaleOffset(init, mdhd.offset));
    }

    static byte[] sidx(int referenceId, long timescale, long earliestTime, long firstOffset,
            List<Reference> references) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] fixed = new byte[SIDX_FIXED_BYTES];
        Mp4Boxes.putU32(fixed, SIDX_REFERENCE_ID, referenceId);
        Mp4Boxes.putU32(fixed, SIDX_TIMESCALE, timescale);
        Mp4Boxes.putU32(fixed, SIDX_EARLIEST_TIME, earliestTime);
        Mp4Boxes.putU32(fixed, SIDX_FIRST_OFFSET, firstOffset);
        Mp4Boxes.putU16(fixed, SIDX_REFERENCE_COUNT, references.size());
        out.write(fixed, 0, fixed.length);
        byte[] entry = new byte[SIDX_REFERENCE_BYTES];
        for (Reference reference : references) {
            Mp4Boxes.putU32(entry, REFERENCE_SIZE_AT, reference.byteLength);
            Mp4Boxes.putU32(entry, REFERENCE_DURATION_AT, reference.duration);
            Mp4Boxes.putU32(entry, REFERENCE_SAP_AT, SAP_FLAGS);
            out.write(entry, 0, entry.length);
        }
        return Mp4Boxes.box("sidx", out.toByteArray());
    }

    private static byte[] sidxBoxFor(Track track, InterleavedTracks ordered, long bytesAfterIndex) throws IOException {
        long mediaTimescale = mediaTimescale(track.init);
        List<Segment> own = new ArrayList<>();
        List<Long> starts = new ArrayList<>();
        long position = 0;
        for (int i = 0; i < ordered.segments.size(); i++) {
            Segment segment = ordered.segments.get(i);
            if (ordered.trackIds.get(i) == track.id) {
                own.add(segment);
                starts.add(position);
            }
            position += segment.payloadBytes();
        }
        long elapsed = own.get(0).startTime;
        long earliestTime = elapsed * mediaTimescale / track.templateTimescale;
        long indexedEnd = earliestTime;
        List<Reference> references = new ArrayList<>();
        for (int i = 0; i < own.size(); i++) {
            long next = (i + 1 < starts.size()) ? starts.get(i + 1) : position;
            elapsed += own.get(i).duration;
            long end = elapsed * mediaTimescale / track.templateTimescale;
            references.add(new Reference(next - starts.get(i), end - indexedEnd));
            indexedEnd = end;
        }
        return sidx(track.id, mediaTimescale, earliestTime, bytesAfterIndex + starts.get(0), references);
    }

    private static byte[] moov(byte[] videoInit, byte[] audioInit, int audioId, long durationMs) throws IOException {
        Mp4Boxes.Box videoMoov = Mp4Boxes.child(videoInit, 0, videoInit.length, "moov");
        Mp4Boxes.Box audioMoov = Mp4Boxes.child(audioInit, 0, audioInit.length, "moov");
        byte[] mvhd = Mp4Boxes.slice(videoInit, Mp4Boxes.childOf(videoInit, videoMoov, "mvhd"));
        long movieTimescale = Mp4Boxes.u32(mvhd, timescaleOffset(mvhd, 0));
        putDuration(mvhd, 0, durationMs * movieTimescale / MILLIS_PER_SECOND);
        Mp4Boxes.putU32(mvhd, mvhd.length - Mp4Boxes.U32_BYTES, audioId + 1);

        byte[] videoTrak = withDurations(Mp4Boxes.slice(videoInit, trak(videoInit)), movieTimescale, durationMs);
        byte[] audioTrak = withDurations(Mp4Boxes.slice(audioInit, trak(audioInit)), movieTimescale, durationMs);
        Mp4Boxes.Box tkhd = Mp4Boxes.child(audioTrak, Mp4Boxes.HEADER_BYTES, audioTrak.length, "tkhd");
        Mp4Boxes.putU32(audioTrak, tkhd.offset + trackIdOffset(audioTrak, tkhd.offset), audioId);

        byte[] videoTrex = trex(videoInit, videoMoov);
        byte[] audioTrex = trex(audioInit, audioMoov);
        Mp4Boxes.putU32(audioTrex, Mp4Boxes.FULL_HEADER_BYTES, audioId);
        return Mp4Boxes.box("moov", Mp4Boxes.concat(mvhd, videoTrak, audioTrak,
                Mp4Boxes.box("mvex", Mp4Boxes.concat(videoTrex, audioTrex))));
    }

    private static Mp4Boxes.Box trak(byte[] init) throws IOException {
        Mp4Boxes.Box moov = Mp4Boxes.child(init, 0, init.length, "moov");
        return Mp4Boxes.childOf(init, moov, "trak");
    }

    private static Mp4Boxes.Box mdhd(byte[] data, Mp4Boxes.Box trak) throws IOException {
        return Mp4Boxes.childOf(data, Mp4Boxes.childOf(data, trak, "mdia"), "mdhd");
    }

    private static byte[] trex(byte[] init, Mp4Boxes.Box moov) throws IOException {
        Mp4Boxes.Box mvex = Mp4Boxes.childOf(init, moov, "mvex");
        return Mp4Boxes.slice(init, Mp4Boxes.childOf(init, mvex, "trex"));
    }

    private static byte[] withDurations(byte[] trak, long movieTimescale, long durationMs) throws IOException {
        Mp4Boxes.Box tkhd = Mp4Boxes.child(trak, Mp4Boxes.HEADER_BYTES, trak.length, "tkhd");
        int tkhdVersion = Mp4Boxes.versionAt(trak, tkhd.offset);
        Mp4Boxes.putDuration(trak, tkhd.offset + ((tkhdVersion == 1) ? TKHD_V1_DURATION : TKHD_V0_DURATION),
                tkhdVersion, durationMs * movieTimescale / MILLIS_PER_SECOND);
        Mp4Boxes.Box mdia = Mp4Boxes.child(trak, Mp4Boxes.HEADER_BYTES, trak.length, "mdia");
        Mp4Boxes.Box mdhd = Mp4Boxes.childOf(trak, mdia, "mdhd");
        long mediaTimescale = Mp4Boxes.u32(trak, mdhd.offset + timescaleOffset(trak, mdhd.offset));
        putDuration(trak, mdhd.offset, durationMs * mediaTimescale / MILLIS_PER_SECOND);
        return trak;
    }

    private static int timescaleOffset(byte[] data, int boxOffset) {
        return (Mp4Boxes.versionAt(data, boxOffset) == 1) ? V1_TIMESCALE : V0_TIMESCALE;
    }

    private static int trackIdOffset(byte[] data, int tkhdOffset) {
        return (Mp4Boxes.versionAt(data, tkhdOffset) == 1) ? TKHD_V1_TRACK_ID : TKHD_V0_TRACK_ID;
    }

    private static void putDuration(byte[] data, int boxOffset, long value) {
        int version = Mp4Boxes.versionAt(data, boxOffset);
        Mp4Boxes.putDuration(data, boxOffset + ((version == 1) ? V1_DURATION : V0_DURATION), version, value);
    }

    static final class Reference {

        final long byteLength;

        final long duration;

        Reference(long byteLength, long duration) {
            this.byteLength = byteLength;
            this.duration = duration;
        }

    }

}

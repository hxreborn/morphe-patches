/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class MovieHeadTest {

    private static final long MOVIE_TIMESCALE = 1000;

    private static final long VIDEO_MEDIA_TIMESCALE = 90000;

    private static final long AUDIO_MEDIA_TIMESCALE = 48000;

    private final byte[] videoInit = Mp4Fixtures.initSegment(1, MOVIE_TIMESCALE, VIDEO_MEDIA_TIMESCALE);

    private final byte[] audioInit = Mp4Fixtures.initSegment(1, MOVIE_TIMESCALE, AUDIO_MEDIA_TIMESCALE);

    @Test
    public void readsTheTrackIdOfAnInitSegment() throws IOException {
        // when
        int trackId = MovieHead.trackId(Mp4Fixtures.initSegment(7, MOVIE_TIMESCALE, VIDEO_MEDIA_TIMESCALE));

        // then
        assertEquals(7, trackId);
    }

    @Test
    public void readsTheMediaTimescaleOfAnInitSegment() throws IOException {
        // when
        long timescale = MovieHead.mediaTimescale(this.videoInit);

        // then
        assertEquals(VIDEO_MEDIA_TIMESCALE, timescale);
    }

    @Test
    public void startsWithTheVideoFileTypeThenTheMovieThenBothIndexes() throws IOException {
        // when
        byte[] head = head();

        // then
        assertEquals(Arrays.asList("ftyp", "moov", "sidx", "sidx"), topLevelTypes(head));
    }

    @Test
    public void setsTheMovieDurationFromTheVideoSegments() throws IOException {
        // when
        Mp4Boxes.Box mvhd = movieChild(head(), "mvhd");

        // then
        assertEquals(6000, Mp4Boxes.u32(head(), mvhd.offset + 24));
    }

    @Test
    public void numbersTheNextTrackAfterTheAudioTrack() throws IOException {
        // when
        Mp4Boxes.Box mvhd = movieChild(head(), "mvhd");

        // then
        assertEquals(3, Mp4Boxes.u32(head(), mvhd.end() - 4));
    }

    @Test
    public void numbersTheAudioTrackAfterTheVideoTrack() throws IOException {
        // given
        byte[] head = head();
        Mp4Boxes.Box moov = Mp4Boxes.child(head, 0, head.length, "moov");
        Mp4Boxes.Box videoTrak = Mp4Boxes.childOf(head, moov, "trak");
        Mp4Boxes.Box audioTrak = secondChild(head, moov, "trak");

        // when
        long videoId = Mp4Boxes.u32(head, Mp4Boxes.childOf(head, videoTrak, "tkhd").offset + 20);
        long audioId = Mp4Boxes.u32(head, Mp4Boxes.childOf(head, audioTrak, "tkhd").offset + 20);

        // then
        assertEquals(1, videoId);
        assertEquals(2, audioId);
    }

    @Test
    public void scalesTheTrackDurationsToEachTimescale() throws IOException {
        // given
        byte[] head = head();
        Mp4Boxes.Box moov = Mp4Boxes.child(head, 0, head.length, "moov");
        Mp4Boxes.Box videoTrak = Mp4Boxes.childOf(head, moov, "trak");
        Mp4Boxes.Box audioTrak = secondChild(head, moov, "trak");

        // when
        long videoTkhd = Mp4Boxes.u32(head, Mp4Boxes.childOf(head, videoTrak, "tkhd").offset + 28);
        long videoMdhd = Mp4Boxes.u32(head, mdhd(head, videoTrak).offset + 24);
        long audioMdhd = Mp4Boxes.u32(head, mdhd(head, audioTrak).offset + 24);

        // then
        assertEquals(6000, videoTkhd);
        assertEquals(540000, videoMdhd);
        assertEquals(288000, audioMdhd);
    }

    @Test
    public void declaresBothTracksInTheMovieExtends() throws IOException {
        // given
        byte[] head = head();
        Mp4Boxes.Box moov = Mp4Boxes.child(head, 0, head.length, "moov");
        Mp4Boxes.Box mvex = Mp4Boxes.childOf(head, moov, "mvex");

        // when
        Mp4Boxes.Box firstTrex = Mp4Boxes.childOf(head, mvex, "trex");
        long first = Mp4Boxes.u32(head, firstTrex.offset + 12);
        long second = Mp4Boxes.u32(head, firstTrex.end() + 12);

        // then
        assertEquals(1, first);
        assertEquals(2, second);
    }

    @Test
    public void indexesEachVideoSegmentWithTheAudioInterleavedAfterIt() throws IOException {
        // given
        byte[] head = head();
        Mp4Boxes.Box videoIndex = topLevel(head, 2);
        Mp4Boxes.Box audioIndex = topLevel(head, 3);

        // then
        assertEquals(1, Mp4Boxes.u32(head, videoIndex.offset + 12));
        assertEquals(VIDEO_MEDIA_TIMESCALE, Mp4Boxes.u32(head, videoIndex.offset + 16));
        assertEquals(0, Mp4Boxes.u32(head, videoIndex.offset + 20));
        assertEquals(audioIndex.size, Mp4Boxes.u32(head, videoIndex.offset + 24));
        assertEquals(Arrays.asList(1200L, 1200L, 1200L), referenceSizes(head, videoIndex));
        assertEquals(Arrays.asList(180000L, 180000L, 180000L), referenceDurations(head, videoIndex));
    }

    @Test
    public void indexesEachAudioSegmentFromTheStartOfThePayloads() throws IOException {
        // given
        byte[] head = head();
        Mp4Boxes.Box audioIndex = topLevel(head, 3);

        // then
        assertEquals(2, Mp4Boxes.u32(head, audioIndex.offset + 12));
        assertEquals(AUDIO_MEDIA_TIMESCALE, Mp4Boxes.u32(head, audioIndex.offset + 16));
        assertEquals(1000, Mp4Boxes.u32(head, audioIndex.offset + 24));
        assertEquals(Arrays.asList(100L, 1100L, 100L, 1100L, 100L, 100L), referenceSizes(head, audioIndex));
        assertEquals(Arrays.asList(48000L, 48000L, 48000L, 48000L, 48000L, 48000L),
                referenceDurations(head, audioIndex));
    }

    @Test
    public void marksEveryReferenceAsStartingWithAStreamAccessPoint() throws IOException {
        // given
        byte[] head = head();
        Mp4Boxes.Box videoIndex = topLevel(head, 2);

        // when
        long flags = Mp4Boxes.u32(head, videoIndex.offset + 32 + 8);

        // then
        assertEquals(0x90000000L, flags);
    }

    @Test
    public void rejectsAnInitSegmentWithoutAMovieBox() {
        // when
        try {
            MovieHead.trackId(Mp4Fixtures.box("ftyp", Mp4Fixtures.zeros(8)));
            org.junit.Assert.fail("expected a missing moov");
        } catch (IOException ex) {

            // then
            assertEquals("missing moov box", ex.getMessage());
        }
    }

    private byte[] head() throws IOException {
        Track video = new Track(this.videoInit, 1, 1000, InterleavedTracksTest.segments("v", 3, 2000, 1000));
        Track audio = new Track(this.audioInit, 2, 1000, InterleavedTracksTest.segments("a", 6, 1000, 100));
        return MovieHead.build(video, audio, InterleavedTracks.interleave(video, audio));
    }

    private static List<String> topLevelTypes(byte[] head) throws IOException {
        List<String> types = new ArrayList<>();
        int at = 0;
        while (at < head.length) {
            types.add(Mp4Boxes.typeAt(head, at));
            at += Mp4Boxes.size(head, at);
        }
        return types;
    }

    private static Mp4Boxes.Box topLevel(byte[] head, int index) throws IOException {
        int at = 0;
        for (int i = 0; i < index; i++) {
            at += Mp4Boxes.size(head, at);
        }
        return new Mp4Boxes.Box(at, Mp4Boxes.size(head, at));
    }

    private static Mp4Boxes.Box movieChild(byte[] head, String type) throws IOException {
        return Mp4Boxes.childOf(head, Mp4Boxes.child(head, 0, head.length, "moov"), type);
    }

    private static Mp4Boxes.Box secondChild(byte[] head, Mp4Boxes.Box parent, String type) throws IOException {
        Mp4Boxes.Box first = Mp4Boxes.childOf(head, parent, type);
        return Mp4Boxes.child(head, first.end(), parent.end(), type);
    }

    private static Mp4Boxes.Box mdhd(byte[] head, Mp4Boxes.Box trak) throws IOException {
        return Mp4Boxes.childOf(head, Mp4Boxes.childOf(head, trak, "mdia"), "mdhd");
    }

    private static List<Long> referenceSizes(byte[] head, Mp4Boxes.Box sidx) {
        return references(head, sidx, 0);
    }

    private static List<Long> referenceDurations(byte[] head, Mp4Boxes.Box sidx) {
        return references(head, sidx, 4);
    }

    private static List<Long> references(byte[] head, Mp4Boxes.Box sidx, int field) {
        int count = (int) (((head[sidx.offset + 30] & 0xFF) << 8) | (head[sidx.offset + 31] & 0xFF));
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(Mp4Boxes.u32(head, sidx.offset + 32 + i * 12 + field));
        }
        return values;
    }

}

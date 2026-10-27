/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class DashFileTest {

    private static final String MANIFEST = "<MPD mediaPresentationDuration=\"PT4S\"><Period>"
            + "<AdaptationSet contentType=\"video\"><SegmentTemplate timescale=\"1000\" initialization=\"v/init.mp4\" "
            + "media=\"v/$Number$.m4s\"><SegmentTimeline><S t=\"0\" d=\"2000\" r=\"1\"/></SegmentTimeline>"
            + "</SegmentTemplate><Representation id=\"v\" height=\"720\" bandwidth=\"1\"/></AdaptationSet>"
            + "<AdaptationSet contentType=\"audio\"><SegmentTemplate timescale=\"48000\" initialization=\"a/init.mp4\" "
            + "media=\"a/$Number$.m4s\" duration=\"96000\"/><Representation id=\"a\" bandwidth=\"1\"/>"
            + "</AdaptationSet></Period></MPD>";

    private static final int TRACK_ID_IN_SEGMENT = 44;

    private final byte[][] videoSegments = { Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(300, 1)),
            Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(310, 2)) };

    private final byte[][] audioSegments = { Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(120, 3)),
            Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(130, 4)) };

    private final AtomicInteger refreshes = new AtomicInteger();

    private LocalCdn cdn;

    @Before
    public void startCdn() throws IOException {
        this.cdn = new LocalCdn("fresh=1");
        this.cdn.put("/index.mpd", MANIFEST.getBytes(StandardCharsets.UTF_8));
        this.cdn.put("/v/init.mp4", Mp4Fixtures.initSegment(1, 1000, 90000));
        this.cdn.put("/a/init.mp4", Mp4Fixtures.initSegment(1, 1000, 48000));
        publishSegments(new byte[0]);
    }

    @After
    public void stopCdn() {
        this.cdn.close();
    }

    @Test
    public void sizesTheFileAsTheHeadPlusEverySegmentPayload() throws IOException {
        // when
        DashFile file = open();

        // then
        assertEquals(headLength(whole(file)) + payloadBytes(), file.length);
    }

    @Test
    public void startsWithTheFileTypeThenTheMovieThenBothIndexes() throws IOException {
        // given
        byte[] bytes = whole(open());

        // when
        int at = 0;
        StringBuilder types = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            types.append(Mp4Boxes.typeAt(bytes, at)).append(' ');
            at += Mp4Boxes.size(bytes, at);
        }

        // then
        assertEquals("ftyp moov sidx sidx ", types.toString());
    }

    @Test
    public void interleavesVideoAndAudioByStartTime() throws IOException {
        // given
        byte[] bytes = whole(open());

        // when
        byte[] body = Arrays.copyOfRange(bytes, headLength(bytes), bytes.length);

        // then
        assertArrayEquals(expectedBody(), body);
    }

    @Test
    public void dropsTheIndexBoxesThatPrecedeEachFragment() throws IOException {
        // given
        byte[] styp = Mp4Fixtures.box("styp", Mp4Fixtures.zeros(8));
        byte[] sidx = Mp4Fixtures.box("sidx", Mp4Fixtures.zeros(32));
        publishSegments(Mp4Boxes.concat(styp, sidx));
        byte[] bytes = whole(open());

        // when
        byte[] body = Arrays.copyOfRange(bytes, headLength(bytes), bytes.length);

        // then
        assertArrayEquals(expectedBody(), body);
    }

    @Test
    public void numbersTheAudioFragmentsAfterTheVideoTrack() throws IOException {
        // given
        byte[] bytes = whole(open());
        int audioStart = headLength(bytes) + this.videoSegments[0].length;

        // when
        long audioTrackId = Mp4Boxes.u32(bytes, audioStart + TRACK_ID_IN_SEGMENT);

        // then
        assertEquals(2, audioTrackId);
    }

    @Test
    public void servesAnyRangeAsTheMatchingSliceOfTheFile() throws IOException {
        // given
        DashFile file = open();
        byte[] whole = whole(file);
        long[][] ranges = { { 0, 9 }, { 100, 499 }, { headLength(whole) - 5, headLength(whole) + 5 },
                { headLength(whole) + 290, headLength(whole) + 700 }, { file.length - 10, file.length - 1 } };

        for (long[] range : ranges) {

            // when
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            file.write(out, range[0], range[1]);

            // then
            assertArrayEquals("range " + range[0] + "-" + range[1],
                    Arrays.copyOfRange(whole, (int) range[0], (int) range[1] + 1), out.toByteArray());
        }
    }

    @Test
    public void renewsTheCookieWhenTheCdnRejectsIt() throws IOException {
        // when
        DashFile file = open();

        // then
        assertTrue(this.refreshes.get() > 0);
        assertTrue(file.length > 0);
    }

    @Test
    public void readsPacedHostsInSmallParallelChunks() throws IOException {
        // when
        String pattern = open().readPattern();

        // then
        assertEquals("16 x 48 KiB reads", pattern);
    }

    @Test
    public void recognizesTheUnpacedHost() {
        // then
        assertTrue(DashFile.isUnpacedCdn("https://sacdn.hakunaymatata.com/abc/index.mpd"));
        assertFalse(DashFile.isUnpacedCdn("https://sbcdn5.hakunaymatata.com/abc/index.mpd"));
    }

    private void publishSegments(byte[] prefix) {
        this.cdn.put("/v/1.m4s", Mp4Boxes.concat(prefix, this.videoSegments[0]));
        this.cdn.put("/v/2.m4s", Mp4Boxes.concat(prefix, this.videoSegments[1]));
        this.cdn.put("/a/1.m4s", Mp4Boxes.concat(prefix, this.audioSegments[0]));
        this.cdn.put("/a/2.m4s", Mp4Boxes.concat(prefix, this.audioSegments[1]));
    }

    private DashFile open() throws IOException {
        CdnCookies cookies = new CdnCookies() {
            @Override
            public String current() {
                return "stale=1";
            }

            @Override
            public String refresh(String rejected) {
                DashFileTest.this.refreshes.incrementAndGet();
                return "fresh=1";
            }
        };
        return DashFile.open(this.cdn.url("/index.mpd"), 720, cookies);
    }

    private static byte[] whole(DashFile file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        file.write(out, 0, file.length - 1);
        return out.toByteArray();
    }

    private static int headLength(byte[] bytes) throws IOException {
        int at = 0;
        for (int i = 0; i < 4; i++) {
            at += Mp4Boxes.size(bytes, at);
        }
        return at;
    }

    private int payloadBytes() {
        int total = 0;
        for (byte[] segment : this.videoSegments) {
            total += segment.length;
        }
        for (byte[] segment : this.audioSegments) {
            total += segment.length;
        }
        return total;
    }

    private byte[] expectedBody() {
        return Mp4Boxes.concat(this.videoSegments[0], asAudio(this.audioSegments[0]), this.videoSegments[1],
                asAudio(this.audioSegments[1]));
    }

    private static byte[] asAudio(byte[] segment) {
        byte[] copy = segment.clone();
        Mp4Boxes.putU32(copy, TRACK_ID_IN_SEGMENT, 2);
        return copy;
    }

}

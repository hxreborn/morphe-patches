/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class DashFileTest {

    private static final int PACED_CHUNK_BYTES = 48 * 1024;

    private DashFixture fixture;

    @Before
    public void startCdn() throws IOException {
        this.fixture = new DashFixture();
    }

    @After
    public void stopCdn() {
        this.fixture.close();
    }

    @Test
    public void sizesTheFileAsTheHeadPlusEverySegmentPayload() throws IOException {
        // when
        DashFile file = open();

        // then
        Assert.assertEquals(headLength(whole(file)) + this.fixture.payloadBytes(), file.length);
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
        Assert.assertEquals("ftyp moov sidx sidx ", types.toString());
    }

    @Test
    public void interleavesVideoAndAudioByStartTime() throws IOException {
        // given
        byte[] bytes = whole(open());

        // when
        byte[] body = Arrays.copyOfRange(bytes, headLength(bytes), bytes.length);

        // then
        Assert.assertArrayEquals(this.fixture.expectedBody(), body);
    }

    @Test
    public void dropsTheIndexBoxesThatPrecedeEachFragment() throws IOException {
        // given
        byte[] styp = Mp4Fixtures.box("styp", Mp4Fixtures.zeros(8));
        byte[] sidx = Mp4Fixtures.box("sidx", Mp4Fixtures.zeros(32));
        this.fixture.publishSegments(Mp4Boxes.concat(styp, sidx));
        byte[] bytes = whole(open());

        // when
        byte[] body = Arrays.copyOfRange(bytes, headLength(bytes), bytes.length);

        // then
        Assert.assertArrayEquals(this.fixture.expectedBody(), body);
    }

    @Test
    public void numbersTheAudioFragmentsAfterTheVideoTrack() throws IOException {
        // given
        byte[] bytes = whole(open());
        int audioStart = headLength(bytes) + this.fixture.videoSegments[0].length;

        // when
        long audioTrackId = Mp4Boxes.u32(bytes, audioStart + DashFixture.TRACK_ID_IN_SEGMENT);

        // then
        Assert.assertEquals(2, audioTrackId);
    }

    @Test
    public void servesAnyRangeAsTheMatchingSliceOfTheFile() throws IOException {
        // given
        DashFile file = open();
        byte[] whole = whole(file);
        long[][] ranges = { { 0, 9 }, { 100, 499 }, { headLength(whole) - 5, headLength(whole) + 5 },
                { headLength(whole) + 290, headLength(whole) + 700 },
                { headLength(whole) + 49000, headLength(whole) + 49300 }, { file.length - 10, file.length - 1 } };

        for (long[] range : ranges) {

            // when
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            file.write(out, range[0], range[1]);

            // then
            Assert.assertArrayEquals("range " + range[0] + "-" + range[1],
                    Arrays.copyOfRange(whole, (int) range[0], (int) range[1] + 1), out.toByteArray());
        }
    }

    @Test
    public void renewsTheCookieWhenTheCdnRejectsIt() throws IOException {
        // when
        DashFile file = open();

        // then
        Assert.assertTrue(this.fixture.refreshes.get() > 0);
        Assert.assertTrue(file.length > 0);
    }

    @Test
    public void readsPacedHostsInSmallParallelChunks() throws IOException {
        // given
        DashFile file = open();
        this.fixture.cdn.delayRanges(100);
        this.fixture.cdn.forgetRanges();

        // when
        byte[] bytes = whole(file);

        // then
        Assert.assertEquals(this.fixture.cdn.rangesOn("/v/1.m4s").toString(), 3,
                this.fixture.cdn.rangesOn("/v/1.m4s").size());
        Assert.assertTrue(largestRange("/v/1.m4s") <= PACED_CHUNK_BYTES);
        Assert.assertTrue(largestRange("/a/2.m4s") <= PACED_CHUNK_BYTES);
        Assert.assertTrue(this.fixture.cdn.peakConcurrentRanges() > 1);
        Assert.assertArrayEquals(this.fixture.expectedBody(),
                Arrays.copyOfRange(bytes, headLength(bytes), bytes.length));
    }

    @Test
    public void recognizesTheUnpacedHost() {
        // then
        Assert.assertTrue(DashFile.isUnpacedCdn("https://sacdn.hakunaymatata.com/abc/index.mpd"));
        Assert.assertFalse(DashFile.isUnpacedCdn("https://sbcdn5.hakunaymatata.com/abc/index.mpd"));
    }

    private int largestRange(String path) {
        int largest = 0;
        for (String range : this.fixture.cdn.rangesOn(path)) {
            String[] bounds = range.split("-");
            largest = Math.max(largest, Integer.parseInt(bounds[1]) - Integer.parseInt(bounds[0]) + 1);
        }
        return largest;
    }

    private DashFile open() throws IOException {
        return this.fixture.open();
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

}

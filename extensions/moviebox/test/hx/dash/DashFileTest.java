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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hx.testing.DashFixture;
import hx.testing.Mp4Fixtures;
import org.assertj.core.api.Assertions;
import org.junit.After;
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
        Assertions.assertThat(file.length).isEqualTo(headLength(whole(file)) + this.fixture.payloadBytes());
    }

    @Test
    public void startsWithTheFileTypeThenTheMovieThenBothIndexes() throws IOException {
        // given
        byte[] bytes = whole(open());

        // when
        String types = headTypes(bytes);

        // then
        Assertions.assertThat(types).isEqualTo("ftyp moov sidx sidx ");
    }

    @Test
    public void interleavesVideoAndAudioByStartTime() throws IOException {
        // given
        byte[] bytes = whole(open());

        // when
        byte[] body = Arrays.copyOfRange(bytes, headLength(bytes), bytes.length);

        // then
        Assertions.assertThat(body).isEqualTo(this.fixture.expectedBody());
    }

    @Test
    public void dropsTheIndexBoxesThatPrecedeEachFragment() throws IOException {
        // given
        byte[] styp = Mp4Fixtures.box("styp", Mp4Fixtures.zeros(8));
        byte[] sidx = Mp4Fixtures.box("sidx", Mp4Fixtures.zeros(32));
        this.fixture.publishSegments(Mp4Fixtures.concat(styp, sidx));
        byte[] bytes = whole(open());

        // when
        byte[] body = Arrays.copyOfRange(bytes, headLength(bytes), bytes.length);

        // then
        Assertions.assertThat(body).isEqualTo(this.fixture.expectedBody());
    }

    @Test
    public void numbersTheAudioFragmentsAfterTheVideoTrack() throws IOException {
        // given
        byte[] bytes = whole(open());
        int audioStart = headLength(bytes) + this.fixture.videoSegments[0].length;

        // when
        long audioTrackId = Mp4Fixtures.readU32(bytes, audioStart + DashFixture.TRACK_ID_IN_SEGMENT);

        // then
        Assertions.assertThat(audioTrackId).isEqualTo(2);
    }

    @Test
    public void servesAnyRangeAsTheMatchingSliceOfTheFile() throws IOException {
        // given
        DashFile file = open();
        byte[] whole = whole(file);
        long[][] ranges = { { 0, 9 }, { 100, 499 }, { headLength(whole) - 5, headLength(whole) + 5 },
                { headLength(whole) + 290, headLength(whole) + 700 },
                { headLength(whole) + 49000, headLength(whole) + 49300 }, { file.length - 10, file.length - 1 } };

        // when
        List<byte[]> served = serve(file, ranges);

        // then
        for (int i = 0; i < ranges.length; i++) {
            Assertions.assertThat(served.get(i))
                .as("range " + ranges[i][0] + "-" + ranges[i][1])
                .isEqualTo(Arrays.copyOfRange(whole, (int) ranges[i][0], (int) ranges[i][1] + 1));
        }
    }

    @Test
    public void renewsTheCookieWhenTheCdnRejectsIt() throws IOException {
        // when
        DashFile file = open();

        // then
        Assertions.assertThat(this.fixture.refreshes.get() > 0).isTrue();
        Assertions.assertThat(file.length > 0).isTrue();
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
        Assertions.assertThat(this.fixture.cdn.rangesOn("/v/1.m4s")).hasSize(3);
        Assertions.assertThat(largestRange("/v/1.m4s") <= PACED_CHUNK_BYTES).isTrue();
        Assertions.assertThat(largestRange("/a/2.m4s") <= PACED_CHUNK_BYTES).isTrue();
        Assertions.assertThat(this.fixture.cdn.peakConcurrentRanges() > 1).isTrue();
        Assertions.assertThat(Arrays.copyOfRange(bytes, headLength(bytes), bytes.length))
            .isEqualTo(this.fixture.expectedBody());
    }

    @Test
    public void recognizesTheUnpacedHost() {
        // when
        boolean unpaced = DashFile.isUnpacedCdn("https://sacdn.hakunaymatata.com/abc/index.mpd");

        // then
        Assertions.assertThat(unpaced).isTrue();
    }

    @Test
    public void recognizesAPacedHost() {
        // when
        boolean unpaced = DashFile.isUnpacedCdn("https://sbcdn5.hakunaymatata.com/abc/index.mpd");

        // then
        Assertions.assertThat(unpaced).isFalse();
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

    private static String headTypes(byte[] bytes) throws IOException {
        int at = 0;
        StringBuilder types = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            types.append(Mp4Fixtures.typeAt(bytes, at)).append(' ');
            at += Mp4Fixtures.sizeAt(bytes, at);
        }
        return types.toString();
    }

    private static List<byte[]> serve(DashFile file, long[][] ranges) throws IOException {
        List<byte[]> served = new ArrayList<>();
        for (long[] range : ranges) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            file.write(out, range[0], range[1]);
            served.add(out.toByteArray());
        }
        return served;
    }

    private static byte[] whole(DashFile file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        file.write(out, 0, file.length - 1);
        return out.toByteArray();
    }

    private static int headLength(byte[] bytes) throws IOException {
        int at = 0;
        for (int i = 0; i < 4; i++) {
            at += Mp4Fixtures.sizeAt(bytes, at);
        }
        return at;
    }

}

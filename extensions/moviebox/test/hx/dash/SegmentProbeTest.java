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
import java.util.ArrayList;
import java.util.Collections;

import hx.mp4.Segment;
import hx.testing.LocalCdn;
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
public final class SegmentProbeTest {

    private static final CdnCookies COOKIES = new CdnCookies() {
        @Override
        public String current() {
            return "c=1";
        }

        @Override
        public String refresh(String rejected) {
            return "c=1";
        }
    };

    private LocalCdn cdn;

    @Before
    public void startCdn() throws IOException {
        this.cdn = new LocalCdn("c=1");
    }

    @After
    public void stopCdn() {
        this.cdn.close();
    }

    @Test
    public void measuresTheWholeSegment() throws IOException {
        // given
        byte[] media = Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(500, 0));
        Segment segment = publish("/s.m4s", media);

        // when
        SegmentProbe.size(Collections.singletonList(segment), new ArrayList<Segment>(), COOKIES);

        // then
        Assertions.assertThat(segment.byteLength).isEqualTo(media.length);
    }

    @Test
    public void findsTheTrackIdOfTheFirstFragment() throws IOException {
        // given
        Segment segment = publish("/s.m4s", Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(500, 0)));

        // when
        SegmentProbe.size(Collections.singletonList(segment), new ArrayList<Segment>(), COOKIES);

        // then
        Assertions.assertThat(segment.prefixBytes).isEqualTo(0);
        Assertions.assertThat(segment.trackIdOffset).isEqualTo(44);
    }

    @Test
    public void countsTheIndexBoxesThatPrecedeTheFragment() throws IOException {
        // given
        byte[] styp = Mp4Fixtures.box("styp", Mp4Fixtures.zeros(8));
        byte[] sidx = Mp4Fixtures.box("sidx", Mp4Fixtures.zeros(32));
        Segment segment = publish("/s.m4s",
                Mp4Fixtures.concat(styp, sidx, Mp4Fixtures.mediaSegment(5, Mp4Fixtures.bytes(500, 0))));

        // when
        SegmentProbe.size(Collections.singletonList(segment), new ArrayList<Segment>(), COOKIES);

        // then
        Assertions.assertThat(segment.prefixBytes).isEqualTo(styp.length + sidx.length);
        Assertions.assertThat(segment.trackIdOffset).isEqualTo(styp.length + sidx.length + 44);
    }

    @Test
    public void countsOnlyTheIndexBoxesThatAreThere() throws IOException {
        // given
        byte[] styp = Mp4Fixtures.box("styp", Mp4Fixtures.zeros(8));
        Segment segment = publish("/s.m4s",
                Mp4Fixtures.concat(styp, Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(500, 0))));

        // when
        SegmentProbe.size(Collections.singletonList(segment), new ArrayList<Segment>(), COOKIES);

        // then
        Assertions.assertThat(segment.prefixBytes).isEqualTo(styp.length);
    }

    @Test
    public void measuresVideoAndAudioSegmentsTogether() throws IOException {
        // given
        Segment video = publish("/v.m4s", Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(100, 0)));
        Segment audio = publish("/a.m4s", Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(200, 0)));

        // when
        SegmentProbe.size(Collections.singletonList(video), Collections.singletonList(audio), COOKIES);

        // then
        Assertions.assertThat(video.byteLength > 0).isTrue();
        Assertions.assertThat(audio.byteLength > video.byteLength).isTrue();
    }

    @Test
    public void rejectsASegmentThatDoesNotStartWithAFragment() {
        // given
        Segment segment = publish("/s.m4s", Mp4Fixtures.box("free", Mp4Fixtures.zeros(300)));

        // when
        Throwable failure = Assertions.catchThrowable(
                () -> SegmentProbe.size(Collections.singletonList(segment), new ArrayList<Segment>(), COOKIES));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage()).isEqualTo("segment does not start with moof");
    }

    private Segment publish(String path, byte[] bytes) {
        this.cdn.put(path, bytes);
        return new Segment(this.cdn.url(path), 0, 1000);
    }

}

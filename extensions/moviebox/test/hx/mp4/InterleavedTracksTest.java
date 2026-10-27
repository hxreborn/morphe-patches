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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class InterleavedTracksTest {

    @Test
    public void placesAudioStartingBeforeTheNextVideoSegmentAfterTheCurrentOne() {
        // given
        Track video = track(1, 1000, segments("v", 3, 2000, 1000));
        Track audio = track(2, 1000, segments("a", 6, 1000, 100));

        // when
        InterleavedTracks ordered = InterleavedTracks.interleave(video, audio);

        // then
        Assertions.assertThat(names(ordered))
            .isEqualTo(Arrays.asList("v0", "a0", "a1", "v1", "a2", "a3", "v2", "a4", "a5"));
        Assertions.assertThat(ordered.trackIds).isEqualTo(Arrays.asList(1, 2, 2, 1, 2, 2, 1, 2, 2));
    }

    @Test
    public void givesTheLastVideoSegmentEveryRemainingAudioSegment() {
        // given
        Track video = track(1, 1000, segments("v", 1, 2000, 1000));
        Track audio = track(2, 1000, segments("a", 5, 1000, 100));

        // when
        InterleavedTracks ordered = InterleavedTracks.interleave(video, audio);

        // then
        Assertions.assertThat(names(ordered)).isEqualTo(Arrays.asList("v0", "a0", "a1", "a2", "a3", "a4"));
    }

    @Test
    public void comparesStartTimesAcrossDifferentTimescales() {
        // given
        Track video = track(1, 1000, segments("v", 2, 2000, 1000));
        Track audio = track(2, 48000, segments("a", 4, 48000, 100));

        // when
        InterleavedTracks ordered = InterleavedTracks.interleave(video, audio);

        // then
        Assertions.assertThat(names(ordered)).isEqualTo(Arrays.asList("v0", "a0", "a1", "v1", "a2", "a3"));
    }

    @Test
    public void keepsVideoOnlyWhenThereIsNoAudio() {
        // given
        Track video = track(1, 1000, segments("v", 2, 2000, 1000));
        Track audio = track(2, 1000, new ArrayList<Segment>());

        // when
        InterleavedTracks ordered = InterleavedTracks.interleave(video, audio);

        // then
        Assertions.assertThat(names(ordered)).isEqualTo(Arrays.asList("v0", "v1"));
    }

    static Track track(int id, long templateTimescale, List<Segment> segments) {
        return new Track(new byte[0], id, templateTimescale, segments);
    }

    static List<Segment> segments(String prefix, int count, long duration, long byteLength) {
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Segment segment = new Segment(prefix + i, i * duration, duration);
            segment.byteLength = byteLength;
            segments.add(segment);
        }
        return segments;
    }

    private static List<String> names(InterleavedTracks ordered) {
        List<String> names = new ArrayList<>();
        for (Segment segment : ordered.segments) {
            names.add(segment.url);
        }
        return names;
    }

}

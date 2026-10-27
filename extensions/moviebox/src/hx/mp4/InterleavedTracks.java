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
import java.util.List;

public final class InterleavedTracks {

    public final List<Segment> segments = new ArrayList<>();

    public final List<Integer> trackIds = new ArrayList<>();

    public static InterleavedTracks interleave(Track video, Track audio) {
        InterleavedTracks ordered = new InterleavedTracks();
        double audioScale = (double) video.templateTimescale / audio.templateTimescale;
        int nextAudio = 0;
        for (int i = 0; i < video.segments.size(); i++) {
            ordered.add(video.segments.get(i), video.id);
            boolean last = i + 1 == video.segments.size();
            long until = last ? Long.MAX_VALUE : video.segments.get(i + 1).startTime;
            while (nextAudio < audio.segments.size()
                    && (last || audio.segments.get(nextAudio).startTime * audioScale < until)) {
                ordered.add(audio.segments.get(nextAudio++), audio.id);
            }
        }
        return ordered;
    }

    private void add(Segment segment, int trackId) {
        this.segments.add(segment);
        this.trackIds.add(trackId);
    }

}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
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

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class InterleavedTracks {

    final List<Segment> segments = new ArrayList<>();

    final List<Integer> trackIds = new ArrayList<>();

    static InterleavedTracks interleave(Track video, Track audio) {
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

    byte[] indexOf(Track track, long bytesAfterIndex) throws IOException {
        long mediaTimescale = MovieHead.mediaTimescale(track.init);
        List<Segment> own = new ArrayList<>();
        List<Long> starts = new ArrayList<>();
        long position = 0;
        for (int i = 0; i < this.segments.size(); i++) {
            Segment segment = this.segments.get(i);
            if (this.trackIds.get(i) == track.id) {
                own.add(segment);
                starts.add(position);
            }
            position += segment.payloadBytes();
        }
        long elapsed = own.get(0).startTime;
        long earliestTime = elapsed * mediaTimescale / track.templateTimescale;
        long indexedEnd = earliestTime;
        List<MovieHead.Reference> references = new ArrayList<>();
        for (int i = 0; i < own.size(); i++) {
            long next = (i + 1 < starts.size()) ? starts.get(i + 1) : position;
            elapsed += own.get(i).duration;
            long end = elapsed * mediaTimescale / track.templateTimescale;
            references.add(new MovieHead.Reference(next - starts.get(i), end - indexedEnd));
            indexedEnd = end;
        }
        return MovieHead.sidx(track.id, mediaTimescale, earliestTime, bytesAfterIndex + starts.get(0), references);
    }

}

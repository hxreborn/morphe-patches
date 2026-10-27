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

import java.util.List;

final class Track {

    final byte[] init;

    final int id;

    final long templateTimescale;

    final List<Segment> segments;

    Track(byte[] init, int id, long templateTimescale, List<Segment> segments) {
        this.init = init;
        this.id = id;
        this.templateTimescale = templateTimescale;
        this.segments = segments;
    }

    long durationMillis() {
        long units = 0;
        for (Segment segment : this.segments) {
            units += segment.duration;
        }
        return units * 1000L / this.templateTimescale;
    }

}

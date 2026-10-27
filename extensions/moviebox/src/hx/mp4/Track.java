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

import java.util.List;

public final class Track {

    final byte[] init;

    final int id;

    final long templateTimescale;

    final List<Segment> segments;

    public Track(byte[] init, int id, long templateTimescale, List<Segment> segments) {
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

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

public final class Segment {

    public final String url;

    public final long startTime;

    public final long duration;

    public long byteLength;

    public int prefixBytes;

    public int trackIdOffset;

    public Segment(String url, long startTime, long duration) {
        this.url = url;
        this.startTime = startTime;
        this.duration = duration;
    }

    public long payloadBytes() {
        return this.byteLength - this.prefixBytes;
    }

}

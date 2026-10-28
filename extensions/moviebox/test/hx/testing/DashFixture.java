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
package hx.testing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import hx.dash.CdnCookies;
import hx.dash.DashFile;

public final class DashFixture implements AutoCloseable {

    public static final int TRACK_ID_IN_SEGMENT = 44;

    private static final String MANIFEST = "<MPD mediaPresentationDuration=\"PT4S\"><Period>"
            + "<AdaptationSet contentType=\"video\"><SegmentTemplate timescale=\"1000\" initialization=\"v/init.mp4\" "
            + "media=\"v/$Number$.m4s\"><SegmentTimeline><S t=\"0\" d=\"2000\" r=\"1\"/></SegmentTimeline>"
            + "</SegmentTemplate><Representation id=\"v\" height=\"720\" bandwidth=\"1\"/></AdaptationSet>"
            + "<AdaptationSet contentType=\"audio\"><SegmentTemplate timescale=\"48000\" initialization=\"a/init.mp4\" "
            + "media=\"a/$Number$.m4s\" duration=\"96000\"/><Representation id=\"a\" bandwidth=\"1\"/>"
            + "</AdaptationSet></Period></MPD>";

    public final LocalCdn cdn;

    public final byte[][] videoSegments = { Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(110_000, 1)),
            Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(120_000, 2)) };

    public final byte[][] audioSegments = { Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(60_000, 3)),
            Mp4Fixtures.mediaSegment(1, Mp4Fixtures.bytes(70_000, 4)) };

    public final AtomicInteger refreshes = new AtomicInteger();

    public DashFixture() throws IOException {
        this.cdn = new LocalCdn("fresh=1");
        this.cdn.put("/index.mpd", MANIFEST.getBytes(StandardCharsets.UTF_8));
        this.cdn.put("/v/init.mp4", Mp4Fixtures.initSegment(1, 1000, 90000));
        this.cdn.put("/a/init.mp4", Mp4Fixtures.initSegment(1, 1000, 48000));
        publishSegments(new byte[0]);
    }

    public void publishSegments(byte[] prefix) {
        this.cdn.put("/v/1.m4s", Mp4Fixtures.concat(prefix, this.videoSegments[0]));
        this.cdn.put("/v/2.m4s", Mp4Fixtures.concat(prefix, this.videoSegments[1]));
        this.cdn.put("/a/1.m4s", Mp4Fixtures.concat(prefix, this.audioSegments[0]));
        this.cdn.put("/a/2.m4s", Mp4Fixtures.concat(prefix, this.audioSegments[1]));
    }

    public DashFile open() throws IOException {
        CdnCookies cookies = new CdnCookies() {
            @Override
            public String current() {
                return "stale=1";
            }

            @Override
            public String refresh(String rejected) {
                DashFixture.this.refreshes.incrementAndGet();
                return "fresh=1";
            }
        };
        return DashFile.open(this.cdn.url("/index.mpd"), 720, cookies);
    }

    public int payloadBytes() {
        int total = 0;
        for (byte[] segment : this.videoSegments) {
            total += segment.length;
        }
        for (byte[] segment : this.audioSegments) {
            total += segment.length;
        }
        return total;
    }

    public byte[] expectedBody() {
        return Mp4Fixtures.concat(this.videoSegments[0], asAudio(this.audioSegments[0]), this.videoSegments[1],
                asAudio(this.audioSegments[1]));
    }

    @Override
    public void close() {
        this.cdn.close();
    }

    private static byte[] asAudio(byte[] segment) {
        byte[] copy = segment.clone();
        Mp4Fixtures.putU32(copy, TRACK_ID_IN_SEGMENT, 2);
        return copy;
    }

}

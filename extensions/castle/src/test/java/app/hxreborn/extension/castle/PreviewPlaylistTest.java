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
package app.hxreborn.extension.castle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Test;

import hx.testing.LocalCdn;

public final class PreviewPlaylistTest {

    private static final String SEGMENT = "https://cdn.test/hls/480/%d.jpg?t=1";

    @After
    public void clearsTheFullHdRequest() {
        FullHdPlaylist.resolution(2);
    }

    @Test
    public void expandsTheFullHdSegmentsWhenAPreviewWasServed() throws IOException {
        try (LocalCdn cdn = new LocalCdn(null)) {
            for (int number = 0; number <= 5; number++) {
                cdn.put("/hls/1080/" + number + ".jpg", new byte[] { 0 });
            }
            String preview = paddedPreview().replace("https://cdn.test", cdn.url(""));
            FullHdPlaylist.resolution(3);

            InputStream result = PreviewPlaylist.expand(cdn.url("/hls/480/index_preview_1.m3u8"),
                    new ByteArrayInputStream(preview.getBytes(StandardCharsets.UTF_8)));

            String expanded = new String(result.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(preview.replace("/480/", "/1080/")
                .replace("#EXTINF:20.000000,\n" + cdn.url("/hls/1080/1.jpg?t=1"),
                        "#EXTINF:5.000000,\n" + cdn.url("/hls/1080/2.jpg?t=1") + "\n#EXTINF:5.000000,\n"
                                + cdn.url("/hls/1080/3.jpg?t=1") + "\n#EXTINF:5.000000,\n"
                                + cdn.url("/hls/1080/4.jpg?t=1") + "\n#EXTINF:5.000000,\n"
                                + cdn.url("/hls/1080/5.jpg?t=1")),
                    expanded);
            assertTrue(cdn.hitsOn("/hls/1080/5.jpg") > 0);
        }
    }

    @Test
    public void expandsTheOriginalPreviewWhenTheFullHdSegmentsAreMissing() throws IOException {
        try (LocalCdn cdn = new LocalCdn(null)) {
            for (int number = 0; number <= 5; number++) {
                cdn.put("/hls/480/" + number + ".jpg", new byte[] { 0 });
            }
            String preview = paddedPreview().replace("https://cdn.test", cdn.url(""));
            FullHdPlaylist.resolution(3);

            InputStream result = PreviewPlaylist.expand(cdn.url("/hls/480/index_preview_1.m3u8"),
                    new ByteArrayInputStream(preview.getBytes(StandardCharsets.UTF_8)));

            String expanded = new String(result.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(expanded.contains(cdn.url("/hls/480/5.jpg?t=1")));
            assertTrue(expanded.endsWith("#EXT-X-ENDLIST\n"));
            assertTrue(cdn.hitsOn("/hls/1080/0.jpg") > 0);
        }
    }

    private static String paddedPreview() {
        return "#EXTM3U\n#EXT-X-TARGETDURATION:12\n#EXTINF:3.000000,\n" + String.format(SEGMENT, 0) + "\n"
                + "#EXTINF:2.500000,\n" + String.format(SEGMENT, 1) + "\n#EXTINF:20.000000,\n"
                + String.format(SEGMENT, 1) + "\n#EXT-X-ENDLIST\n";
    }

    @Test
    public void replacesThePaddingWithTheRemainingSegments() {
        // given
        PreviewPlaylist.Padding padding = PreviewPlaylist.Padding.of(paddedPreview());

        // when
        String expanded = padding.expanded(5);

        // then
        assertEquals("#EXTM3U\n#EXT-X-TARGETDURATION:12\n#EXTINF:3.000000,\n" + String.format(SEGMENT, 0) + "\n"
                + "#EXTINF:2.500000,\n" + String.format(SEGMENT, 1) + "\n#EXTINF:5.000000,\n"
                + String.format(SEGMENT, 2) + "\n#EXTINF:5.000000,\n" + String.format(SEGMENT, 3)
                + "\n#EXTINF:5.000000,\n" + String.format(SEGMENT, 4) + "\n#EXTINF:5.000000,\n"
                + String.format(SEGMENT, 5) + "\n#EXT-X-ENDLIST\n", expanded);
    }

    @Test
    public void readsTheLastServedSegment() {
        // when
        PreviewPlaylist.Padding padding = PreviewPlaylist.Padding.of(paddedPreview());

        // then
        assertEquals(1, padding.lastServed);
    }

    @Test
    public void readsThePaddedDuration() {
        // when
        PreviewPlaylist.Padding padding = PreviewPlaylist.Padding.of(paddedPreview());

        // then
        assertEquals(20.0, padding.seconds, 0.0);
    }

    @Test
    public void ignoresAPlaylistWithoutARepeatedLastSegment() {
        // given
        String full = "#EXTM3U\n#EXTINF:3.0,\n" + String.format(SEGMENT, 0) + "\n#EXTINF:3.0,\n"
                + String.format(SEGMENT, 1) + "\n#EXT-X-ENDLIST\n";

        // when
        PreviewPlaylist.Padding padding = PreviewPlaylist.Padding.of(full);

        // then
        assertNull(padding);
    }

    @Test
    public void passesTheSameStreamThroughForALocationThatIsNotAPreview() throws IOException {
        // given
        InputStream playlist = new ByteArrayInputStream(paddedPreview().getBytes(StandardCharsets.UTF_8));

        // when
        InputStream result = PreviewPlaylist.expand("https://cdn.test/hls/480/index_1.m3u8", playlist);

        // then
        assertSame(playlist, result);
    }

    @Test
    public void returnsTheOriginalBytesWhenADurationIsMalformed() throws IOException {
        // given
        String malformed = paddedPreview().replace("#EXTINF:20.000000,", "#EXTINF:oops,");
        byte[] original = malformed.getBytes(StandardCharsets.UTF_8);

        // when
        InputStream result = PreviewPlaylist.expand("https://cdn.test/hls/480/index_preview_1.m3u8",
                new ByteArrayInputStream(original));

        // then
        assertEquals(new String(original, StandardCharsets.UTF_8),
                new String(result.readAllBytes(), StandardCharsets.UTF_8));
    }

}

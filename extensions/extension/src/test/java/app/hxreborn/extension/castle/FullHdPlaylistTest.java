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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Test;

public final class FullHdPlaylistTest {

    @After
    public void clearsTheFullHdRequest() {
        FullHdPlaylist.resolution(2);
    }

    @Test
    public void swapsTheHashedTierDirectory() {
        // when
        String url = FullHdPlaylist.fullHdUrl("https://cdn.test/hls_mps/abc/720/12.jpg?t=1");

        // then
        assertEquals("https://cdn.test/hls_mps/abc/1080/12.jpg?t=1", url);
    }

    @Test
    public void swapsTheNamedTierDirectory() {
        // when
        String url = FullHdPlaylist.fullHdUrl("https://cdn.test/myhls/2026/10/Film/Telug_720ts/s_3.jpg?t=1");

        // then
        assertEquals("https://cdn.test/myhls/2026/10/Film/Telug_1080ts/s_3.jpg?t=1", url);
    }

    @Test
    public void ignoresASegmentOutsideTheHdTier() {
        // when
        String url = FullHdPlaylist.fullHdUrl("https://cdn.test/hls_mps/abc/480/12.jpg?t=1");

        // then
        assertNull(url);
    }

    @Test
    public void mapsTheFullHdPickToTheHdRequest() {
        // when
        Integer requested = FullHdPlaylist.resolution(3);

        // then
        assertEquals(Integer.valueOf(2), requested);
    }

    @Test
    public void keepsAnotherResolutionUnchanged() {
        // when
        Integer requested = FullHdPlaylist.resolution(1);

        // then
        assertEquals(Integer.valueOf(1), requested);
    }

    @Test
    public void passesThePlaylistThroughWhenFullHdWasNotPicked() throws IOException {
        // given
        FullHdPlaylist.resolution(2);
        InputStream playlist = new ByteArrayInputStream(new byte[0]);

        // when
        InputStream result = FullHdPlaylist.expand(playlist);

        // then
        assertSame(playlist, result);
    }

    @Test
    public void keepsTheRequestWhenAnotherRequestHasNoResolution() {
        // given
        FullHdPlaylist.resolution(3);

        // when
        Integer requested = FullHdPlaylist.resolution(null);

        // then
        assertEquals(Integer.valueOf(2), requested);
    }

    @Test
    public void swapsTheTierWhenBothTiersHaveTheSameSegments() throws IOException {
        // given
        UrlExistence existence = existingUpTo(3);

        // when
        String playlist = FullHdPlaylist.fullHd(hdPlaylist(4), existence);

        // then
        assertEquals("#EXTM3U\n#EXT-X-TARGETDURATION:10\n" + segment(1.0, 0) + segment(2.0, 1) + segment(3.0, 2)
                + segment(4.0, 3) + "#EXT-X-ENDLIST\n", playlist);
    }

    @Test
    public void rewritesEverySegmentWhenTheFullHdTierHasMoreSegments() throws IOException {
        // given
        UrlExistence existence = existingUpTo(7);

        // when
        String playlist = FullHdPlaylist.fullHd(hdPlaylist(4), existence);

        // then
        assertEquals("#EXTM3U\n#EXT-X-TARGETDURATION:10\n" + segment(1.0, 0) + segment(2.0, 1) + segment(3.0, 2)
                + segment(4.0, 3) + "#EXT-X-ENDLIST\n", playlist);
    }

    @Test
    public void endsThePlaylistAtTheLastFullHdSegment() throws IOException {
        // given
        UrlExistence existence = existingUpTo(1);

        // when
        String playlist = FullHdPlaylist.fullHd(hdPlaylist(4), existence);

        // then
        assertEquals("#EXTM3U\n#EXT-X-TARGETDURATION:10\n" + segment(1.0, 0) + segment(2.0, 1) + "#EXT-X-ENDLIST\n",
                playlist);
    }

    @Test
    public void keepsTheHdPlaylistWhenTheFullHdTierHasNoFirstSegment() throws IOException {
        // given
        UrlExistence existence = existingUpTo(-1);

        // when
        String playlist = FullHdPlaylist.fullHd(hdPlaylist(4), existence);

        // then
        assertNull(playlist);
    }

    private static String segment(double seconds, int number) {
        return String.format(java.util.Locale.ROOT, "#EXTINF:%.6f,\nhttps://cdn.test/hls/abc/1080/%d.jpg?t=1\n",
                seconds, number);
    }

    private static String hdPlaylist(int segments) {
        StringBuilder playlist = new StringBuilder("#EXTM3U\n#EXT-X-TARGETDURATION:10\n");
        for (int number = 0; number < segments; number++) {
            playlist.append(String.format(java.util.Locale.ROOT,
                    "#EXTINF:%.6f,\nhttps://cdn.test/hls/abc/720/%d.jpg?t=1\n", number + 1.0, number));
        }
        return playlist.append("#EXT-X-ENDLIST\n").toString();
    }

    private static UrlExistence existingUpTo(final int lastNumber) {
        return new UrlExistence() {

            @Override
            public boolean exists(String url) {
                int slash = url.lastIndexOf('/');
                int number = Integer.parseInt(url.substring(slash + 1, url.indexOf('.', slash)));
                return url.contains("/1080/") && number <= lastNumber;
            }

        };
    }

}

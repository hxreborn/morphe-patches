/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.terabox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class TeraboxHdUnlockTest {

    private static final int RECORDED_FILES = 32;

    private static final String PROXY_PREFIX = "http://127.0.0.1:";

    private static String streamingUrl(String path, String type) {
        return "https://www.terabox.com/api/streaming?path=" + Uri.encode(path) + "&type=" + type;
    }

    private static String sharedStreamingUrl(String fsId, String type) {
        return "https://www.terabox.com/share/streaming?fid=" + fsId + "&type=" + type;
    }

    private static boolean isRecorded(String path) {
        TeraboxHdUnlock.selectPlaybackUrl(null, streamingUrl(path, "M3U8_AUTO_480"));
        return TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_720);
    }

    private static boolean isSharedFileRecorded(String fsId) {
        TeraboxHdUnlock.selectPlaybackUrl(null, sharedStreamingUrl(fsId, "M3U8_AUTO_480"));
        return TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_720);
    }

    private static boolean unlocksTier(String tier) {
        final String path = "/tiers/" + tier;
        TeraboxHdUnlock.recordOriginalFile(path, "https://dl/" + tier);
        final String url = streamingUrl(path, tier);
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(new Player(), url);
        if (selected.startsWith(PROXY_PREFIX)) {
            return true;
        }
        assertSame(tier, url, selected);
        return false;
    }

    @Test
    public void treatsTiersFromSevenTwentyUpAsHd() {
        for (String tier : new String[] { "720", "1080", "1440", "2160", "M3U8_AUTO_720", "M3U8_AUTO_1080", "1080P",
                "0720", "ORIGIN", "M3U8_ORIGIN", "4K", "M3U8_AUTO_4K", "8K" }) {
            assertTrue(tier, unlocksTier(tier));
        }
    }

    @Test
    public void treatsLowerOrUnparsableTiersAsStandard() {
        for (String tier : new String[] { "719", "480", "360", "240", "M3U8_AUTO_480", "M3U8_AUTO_360", "AUTO",
                "M3U8_AUTO", "M3U8_AUTO_", "", "_", "P720", "origin", "4k" }) {
            assertFalse(tier, unlocksTier(tier));
        }
    }

    @Test
    public void treatsAMissingTierAsStandard() {
        // given
        TeraboxHdUnlock.recordOriginalFile("/missing-tier.mp4", "https://dl/missing-tier");
        final String url = "https://www.terabox.com/api/streaming?path=%2Fmissing-tier.mp4";

        // when
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(new Player(), url);

        // then
        assertSame(url, selected);
    }

    @Test
    public void requiresReloadForHdResolutionsOnlyWhileTheOriginalIsRecorded() {
        TeraboxHdUnlock.recordOriginalFile("/reload-hd.mp4", "https://dl/reload-hd");
        TeraboxHdUnlock.selectPlaybackUrl(null, streamingUrl("/reload-hd.mp4", "M3U8_AUTO_480"));
        assertTrue(TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_720));
        assertTrue(TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_1080));
        assertTrue(TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_ORIGIN));
        assertFalse(TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_480));

        TeraboxHdUnlock.selectPlaybackUrl(null, streamingUrl("/reload-unrecorded.mp4", "M3U8_AUTO_480"));
        assertFalse(TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_720));
    }

    @Test
    public void requiresReloadToLeaveTheOriginalStream() {
        TeraboxHdUnlock.recordOriginalFile("/reload-leave.mp4", "https://dl/reload-leave");
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(new Player(),
                streamingUrl("/reload-leave.mp4", "M3U8_AUTO_1080"));
        assertTrue(selected, selected.startsWith(PROXY_PREFIX));
        assertTrue(TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_480));

        TeraboxHdUnlock.selectPlaybackUrl(null, streamingUrl("/reload-leave.mp4", "M3U8_AUTO_480"));
        assertFalse(TeraboxHdUnlock.requiresReload(Resolution.M3U8_AUTO_480));
    }

    @Test
    public void requiresNoReloadAfterANonStreamingUrl() {
        // given
        TeraboxHdUnlock.recordOriginalFile("/reload-none.mp4", "https://dl/reload-none");
        TeraboxHdUnlock.selectPlaybackUrl(null, streamingUrl("/reload-none.mp4", "M3U8_AUTO_480"));

        // when
        TeraboxHdUnlock.selectPlaybackUrl(null, "https://cdn.example/video.mp4");

        // then
        for (Resolution resolution : Resolution.values()) {
            assertFalse(resolution.name(), TeraboxHdUnlock.requiresReload(resolution));
        }
    }

    @Test
    public void passesNonStreamingUrlsThrough() {
        final Player player = new Player();
        for (String url : new String[] { null, "", "https://cdn.example/video.mp4",
                "https://www.terabox.com/api/list?path=%2Fa", "https://www.terabox.com/share/list?shareid=1" }) {
            assertEquals(url, TeraboxHdUnlock.selectPlaybackUrl(player, url));
        }
        assertEquals(0, player.optionCalls);
    }

    @Test
    public void passesTheSameUrlInstanceThrough() {
        // given
        final String url = new String("https://cdn.example/video.mp4");

        // when
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(null, url);

        // then
        assertSame(url, selected);
    }

    @Test
    public void passesUnrecordedFilesThrough() {
        // given
        final Player player = new Player();
        final String url = streamingUrl("/never-recorded.mp4", "M3U8_AUTO_1080");

        // when
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(player, url);

        // then
        assertSame(url, selected);
        assertEquals(0, player.optionCalls);
    }

    @Test
    public void passesStreamingUrlsWithoutAFileThrough() {
        // given
        final String url = "https://www.terabox.com/api/streaming?type=M3U8_AUTO_1080";

        // when
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(new Player(), url);

        // then
        assertSame(url, selected);
    }

    @Test
    public void proxiesTheRecordedOriginalForHdTiers() throws IOException {
        // given
        final Player player = new Player();
        TeraboxHdUnlock.recordOriginalFile("/proxied.mp4", "https://dl/proxied");

        // when
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(player,
                streamingUrl("/proxied.mp4", "M3U8_AUTO_1080"));

        // then
        assertEquals(ParallelRangeProxy.proxyUrl("https://dl/proxied"), selected);
        assertFalse(player.dashP2p);
        assertFalse(player.customHls);
        assertEquals(2, player.optionCalls);
    }

    @Test
    public void leavesThePlayerAloneForStandardTiers() {
        // given
        final Player player = new Player();
        TeraboxHdUnlock.recordOriginalFile("/standard.mp4", "https://dl/standard");

        // when
        TeraboxHdUnlock.selectPlaybackUrl(player, streamingUrl("/standard.mp4", "M3U8_AUTO_480"));

        // then
        assertTrue(player.dashP2p);
        assertTrue(player.customHls);
    }

    @Test
    public void stillProxiesWhenThePlayerLacksTheOptionSetters() {
        // given
        TeraboxHdUnlock.recordOriginalFile("/bare-player.mp4", "https://dl/bare-player");

        // when
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(new Object(),
                streamingUrl("/bare-player.mp4", "M3U8_AUTO_1080"));

        // then
        assertTrue(selected, selected.startsWith(PROXY_PREFIX));
    }

    @Test
    public void proxiesEachFileSeparately() {
        // given
        TeraboxHdUnlock.recordOriginalFile("/pair-a.mp4", "https://dl/pair-a");
        TeraboxHdUnlock.recordOriginalFile("/pair-b.mp4", "https://dl/pair-b");
        final String first = TeraboxHdUnlock.selectPlaybackUrl(new Player(),
                streamingUrl("/pair-a.mp4", "M3U8_AUTO_1080"));

        // when
        final String second = TeraboxHdUnlock.selectPlaybackUrl(new Player(),
                streamingUrl("/pair-b.mp4", "M3U8_AUTO_1080"));

        // then
        assertNotEquals(first, second);
    }

    @Test
    public void recordsTheOriginalLinkPerFile() {
        TeraboxHdUnlock.recordOriginalFile("/movies/a.mp4", "https://dl/a");
        TeraboxHdUnlock.recordOriginalFile("/movies/b.mp4", "https://dl/b");
        assertTrue(isRecorded("/movies/a.mp4"));
        assertTrue(isRecorded("/movies/b.mp4"));
        assertFalse(isRecorded("/movies/c.mp4"));
    }

    @Test
    public void replacesTheLinkWhenAFileIsRecordedAgain() throws IOException {
        // given
        TeraboxHdUnlock.recordOriginalFile("/replaced.mp4", "https://dl/old");

        // when
        TeraboxHdUnlock.recordOriginalFile("/replaced.mp4", "https://dl/new");

        // then
        final String selected = TeraboxHdUnlock.selectPlaybackUrl(new Player(),
                streamingUrl("/replaced.mp4", "M3U8_AUTO_1080"));
        assertEquals(ParallelRangeProxy.proxyUrl("https://dl/new"), selected);
    }

    @Test
    public void ignoresRecordsWithoutAKeyOrALink() {
        TeraboxHdUnlock.recordOriginalFile(null, "https://dl/a");
        TeraboxHdUnlock.recordOriginalFile("/ignored-a.mp4", null);
        TeraboxHdUnlock.recordOriginalFile("/ignored-b.mp4", "");
        TeraboxHdUnlock.recordSharedFile(null, "https://dl/c");
        TeraboxHdUnlock.recordSharedFile("4242", null);
        TeraboxHdUnlock.recordSharedFile("4243", "");
        assertFalse(isRecorded("/ignored-a.mp4"));
        assertFalse(isRecorded("/ignored-b.mp4"));
        assertFalse(isSharedFileRecorded("null"));
        assertFalse(isSharedFileRecorded("4242"));
        assertFalse(isSharedFileRecorded("4243"));
    }

    @Test
    public void keepsSharedFilesApartFromOwnFiles() {
        TeraboxHdUnlock.recordSharedFile("42", "https://dl/shared");
        assertTrue(isSharedFileRecorded("42"));
        assertFalse(isRecorded("42"));
        assertFalse(isSharedFileRecorded("43"));

        TeraboxHdUnlock.recordOriginalFile("77", "https://dl/own");
        assertTrue(isRecorded("77"));
        assertFalse(isSharedFileRecorded("77"));
    }

    @Test
    public void forgetsTheOldestFilesBeyondTheLimit() {
        // when
        for (int i = 0; i < RECORDED_FILES + 8; i++) {
            TeraboxHdUnlock.recordOriginalFile("/limit/f" + i, "https://dl/" + i);
        }

        // then
        assertFalse(isRecorded("/limit/f0"));
        assertFalse(isRecorded("/limit/f7"));
        assertTrue(isRecorded("/limit/f8"));
        assertTrue(isRecorded("/limit/f39"));
    }

    @Test
    public void recordingAgainKeepsAFileFromBeingForgotten() {
        // given
        for (int i = 0; i < RECORDED_FILES; i++) {
            TeraboxHdUnlock.recordOriginalFile("/keep/f" + i, "https://dl/" + i);
        }
        TeraboxHdUnlock.recordOriginalFile("/keep/f0", "https://dl/0");

        // when
        TeraboxHdUnlock.recordOriginalFile("/keep/extra", "https://dl/extra");

        // then
        assertFalse(isRecorded("/keep/f1"));
        assertTrue(isRecorded("/keep/f0"));
        assertTrue(isRecorded("/keep/extra"));
        assertTrue(isRecorded("/keep/f31"));
    }

    @Test
    public void selectingAFileKeepsItFromBeingForgotten() {
        for (int i = 0; i < RECORDED_FILES; i++) {
            TeraboxHdUnlock.recordOriginalFile("/recent/f" + i, "https://dl/" + i);
        }
        assertTrue(isRecorded("/recent/f0"));
        TeraboxHdUnlock.recordOriginalFile("/recent/extra", "https://dl/extra");
        assertTrue(isRecorded("/recent/f0"));
        assertFalse(isRecorded("/recent/f1"));
    }

    public static final class Player {

        boolean dashP2p = true;

        boolean customHls = true;

        int optionCalls;

        public void setEnableDashP2P(boolean enabled) {
            this.dashP2p = enabled;
            this.optionCalls++;
        }

        public void setEnableCustomHls(boolean enabled) {
            this.customHls = enabled;
            this.optionCalls++;
        }

    }

    private enum Resolution {

        M3U8_AUTO_480, M3U8_AUTO_720, M3U8_AUTO_1080, M3U8_AUTO_ORIGIN

    }

}

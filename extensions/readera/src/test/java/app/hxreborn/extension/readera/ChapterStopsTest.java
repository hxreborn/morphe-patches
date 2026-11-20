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
package app.hxreborn.extension.readera;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ChapterStopsTest {

    private static final int GENERATION = 5;

    private static final int START_PAGE = 2;

    private static final int STOP_PAGE = 12;

    private static final double BOUNDARY_RATIO = 0.5;

    private static final int LAYOUT = 1;

    private static final int NEW_LAYOUT = 2;

    private static final Position FIRST = position(START_PAGE, 0);

    private static final Position CHILD = new Position(START_PAGE, 0, "child", 1, null);

    private static final Position SIBLING = new Position(START_PAGE, 0, "sibling", 1, null);

    private static final Position THIRD = position(STOP_PAGE, BOUNDARY_RATIO);

    private static final Position LINK = new Position(STOP_PAGE, BOUNDARY_RATIO, "link", 0, "link");

    private static final Position EARLIER = position(0, 0);

    private static final Track TRACK = track(GENERATION, THIRD);

    private ChapterStops stops;

    @Before
    public void requestChapter() {
        this.stops = new ChapterStops();
        this.stops.request(FIRST, FIRST);
        Assert.assertTrue(this.stops.onSpeechStart(FIRST, false, GENERATION));
    }

    @Test
    public void includesDeeperChildren() {
        Assert.assertSame(THIRD, this.stops.chapterEnd(Arrays.asList(FIRST, CHILD, THIRD)));
    }

    @Test
    public void stopsChildAtNextSibling() {
        this.stops.request(CHILD, CHILD);
        this.stops.onSpeechStart(CHILD, false, GENERATION);
        Assert.assertSame(SIBLING, this.stops.chapterEnd(Arrays.asList(FIRST, CHILD, SIBLING, THIRD)));
    }

    @Test
    public void skipsLinkEntries() {
        Assert.assertSame(THIRD, this.stops.chapterEnd(Arrays.asList(FIRST, LINK, THIRD)));
    }

    @Test
    public void skipsOutOfOrderEntries() {
        Assert.assertSame(THIRD, this.stops.chapterEnd(Arrays.asList(FIRST, EARLIER, THIRD)));
    }

    @Test
    public void readsLastChapterToBookEnd() {
        this.stops.chapterEnd(Arrays.asList(FIRST, CHILD));
        Assert.assertEquals(StopNotice.BOOK_END, this.stops.setChapterEnd(null, null));
        Assert.assertSame(TRACK, this.stops.nextTrack(TRACK));
    }

    @Test
    public void usesTenPagesForMissingEntry() {
        this.stops.chapterEnd(Arrays.asList(THIRD));
        Assert.assertEquals(StopNotice.MISSING_CHAPTER, this.stops.setChapterEnd(null, null));
        Assert.assertNull(this.stops.nextTrack(track(GENERATION, position(STOP_PAGE, 1))));
    }

    @Test
    public void preventsLookAheadWithoutConsumingStop() {
        armExact(this.stops);
        Assert.assertNull(this.stops.lookAhead(THIRD, GENERATION));
        Assert.assertNull(this.stops.lookAhead(THIRD, GENERATION));
        Assert.assertSame(FIRST, this.stops.lookAhead(FIRST, GENERATION));
        Assert.assertNull(this.stops.nextTrack(TRACK));
    }

    @Test
    public void keepsOtherGenerationLookAhead() {
        armExact(this.stops);
        Assert.assertSame(THIRD, this.stops.lookAhead(THIRD, GENERATION - 1));
        Assert.assertSame(THIRD, this.stops.lookAhead(THIRD, GENERATION + 1));
        Assert.assertNull(this.stops.nextTrack(TRACK));
    }

    @Test
    public void pausesAtEqualUtterance() {
        armExact(this.stops);
        Assert.assertNull(this.stops.nextTrack(track(GENERATION, position(STOP_PAGE, BOUNDARY_RATIO))));
    }

    @Test
    public void pausesAtLaterRatio() {
        armExact(this.stops);
        Assert.assertNull(this.stops.nextTrack(track(GENERATION, position(STOP_PAGE + 1, 1))));
    }

    @Test
    public void keepsLaterTrackAtSameRatio() {
        armExact(this.stops);
        Track later = track(GENERATION, new Position(STOP_PAGE, BOUNDARY_RATIO, "different"));
        Assert.assertSame(later, this.stops.nextTrack(later));
    }

    @Test
    public void pausesAtPageBoundary() {
        armPages(this.stops);
        Assert.assertNull(this.stops.nextTrack(track(GENERATION, position(STOP_PAGE, 1))));
    }

    @Test
    public void ignoresOlderGeneration() {
        armExact(this.stops);
        Track older = track(GENERATION - 1, THIRD);
        Assert.assertSame(older, this.stops.nextTrack(older));
        Assert.assertNull(this.stops.nextTrack(TRACK));
    }

    @Test
    public void clearsForNewerGeneration() {
        armExact(this.stops);
        Track newer = track(GENERATION + 1, THIRD);
        Assert.assertSame(newer, this.stops.nextTrack(newer));
        Assert.assertSame(TRACK, this.stops.nextTrack(TRACK));
    }

    @Test
    public void carriesStopDuringRestart() {
        armExact(this.stops);
        Assert.assertFalse(this.stops.onSpeechStart(THIRD, true, GENERATION + 1));
        Assert.assertNull(this.stops.nextTrack(track(GENERATION + 1, THIRD)));
    }

    @Test
    public void checksTrackFromSupersededRestart() {
        armExact(this.stops);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 1);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 2);
        Assert.assertNull(this.stops.nextTrack(track(GENERATION + 1, THIRD)));
    }

    @Test
    public void checksLookAheadFromSupersededRestart() {
        armExact(this.stops);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 1);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 2);
        Assert.assertNull(this.stops.lookAhead(THIRD, GENERATION + 1));
    }

    @Test
    public void returnsBoundaryForRestartedGeneration() {
        armExact(this.stops);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 1);
        Assert.assertSame(THIRD, this.stops.restartBoundary(GENERATION + 1));
        Assert.assertNull(this.stops.restartBoundary(GENERATION));
    }

    @Test
    public void carriesPageStopWithoutBoundary() {
        armPages(this.stops);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 1);
        Assert.assertNull(this.stops.restartBoundary(GENERATION + 1));
        Assert.assertNull(this.stops.nextTrack(track(GENERATION + 1, position(STOP_PAGE, 1))));
    }

    @Test
    public void keepsUtteranceWhenRestartResolvesNothing() {
        armExact(this.stops);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 1);
        this.stops.setRestartUtterance(THIRD, null, GENERATION + 1);
        Assert.assertNull(this.stops.nextTrack(track(GENERATION + 1, THIRD)));
    }

    @Test
    public void ignoresRestartUtteranceForOtherGeneration() {
        armExact(this.stops);
        this.stops.onSpeechStart(THIRD, true, GENERATION + 1);
        Position other = new Position(STOP_PAGE, BOUNDARY_RATIO, "other heading");
        this.stops.setRestartUtterance(THIRD, other, GENERATION);
        Assert.assertNull(this.stops.nextTrack(track(GENERATION + 1, THIRD)));
    }

    @Test
    public void clearsForUnrelatedStart() {
        armExact(this.stops);
        Assert.assertFalse(this.stops.onSpeechStart(THIRD, false, GENERATION + 1));
        Assert.assertSame(TRACK, this.stops.nextTrack(TRACK));
    }

    @Test
    public void playsSkippedBoundary() {
        armExact(this.stops);
        this.stops.beginManualAdvance();
        Assert.assertSame(TRACK, this.stops.nextTrack(TRACK));
        Assert.assertSame(TRACK, this.stops.nextTrack(TRACK));
    }

    @Test
    public void keepsStopAfterSkipInsideChapter() {
        armExact(this.stops);
        this.stops.beginManualAdvance();
        Track inside = track(GENERATION, FIRST);
        Assert.assertSame(inside, this.stops.nextTrack(inside));
        this.stops.endManualAdvance();
        Assert.assertNull(this.stops.nextTrack(TRACK));
    }

    @Test
    public void passesNullTrack() {
        armExact(this.stops);
        Assert.assertNull(this.stops.nextTrack(null));
        Assert.assertNull(this.stops.nextTrack(TRACK));
    }

    @Test
    public void usesTenPagesWithoutContents() {
        this.stops.request(FIRST, null);
        Assert.assertTrue(this.stops.onSpeechStart(FIRST, false, GENERATION));
        Assert.assertEquals(StopNotice.TEN_PAGES, this.stops.setChapterEnd(null, null));
        Assert.assertNull(this.stops.nextTrack(track(GENERATION, position(STOP_PAGE, 1))));
    }

    @Test
    public void usesNextPageForSamePageFallback() {
        Position samePageEnd = new Position(START_PAGE, 0, "next chapter");
        this.stops.chapterEnd(Arrays.asList(FIRST, samePageEnd));
        this.stops.setChapterEnd(samePageEnd, null);
        Track currentPage = track(GENERATION, FIRST);
        Assert.assertSame(currentPage, this.stops.nextTrack(currentPage));
        Assert.assertNull(this.stops.nextTrack(track(GENERATION, position(START_PAGE + 1, 0))));
    }

    @Test
    public void clearsStopAfterAutomaticPause() {
        armExact(this.stops);
        Assert.assertNull(this.stops.nextTrack(TRACK));
        Assert.assertSame(TRACK, this.stops.nextTrack(TRACK));
    }

    @Test
    public void rejectsEqualStartWithDifferentIdentity() {
        this.stops.request(FIRST, FIRST);
        Assert.assertFalse(this.stops.onSpeechStart(position(START_PAGE, 0), false, GENERATION));
        Assert.assertTrue(this.stops.onSpeechStart(FIRST, false, GENERATION));
    }

    @Test
    public void retainsPendingRequestWhenOlderStartArrives() {
        this.stops.request(FIRST, FIRST);
        this.stops.request(CHILD, CHILD);
        Assert.assertFalse(this.stops.onSpeechStart(FIRST, false, GENERATION));
        Assert.assertTrue(this.stops.onSpeechStart(CHILD, false, GENERATION + 1));
        Assert.assertSame(THIRD, this.stops.chapterEnd(Arrays.asList(FIRST, CHILD, THIRD)));
        this.stops.setChapterEnd(THIRD, THIRD);
        Assert.assertNull(this.stops.nextTrack(track(GENERATION + 1, THIRD)));
    }

    @Test
    public void replacesPendingRequest() {
        this.stops.request(FIRST, FIRST);
        this.stops.request(THIRD, THIRD);
        Assert.assertTrue(this.stops.onSpeechStart(THIRD, false, GENERATION));
        Assert.assertNull(this.stops.chapterEnd(Arrays.asList(FIRST, THIRD)));
        Assert.assertEquals(StopNotice.BOOK_END, this.stops.setChapterEnd(null, null));
    }

    @Test
    public void usesRestartUtteranceInNewLayout() {
        armExact(this.stops);
        this.stops.onSpeechStart(FIRST, true, GENERATION + 1);
        Position relaidOut = new Position(STOP_PAGE + 2, 0.9, "chapter heading", NEW_LAYOUT);
        this.stops.setRestartUtterance(this.stops.restartBoundary(GENERATION + 1), relaidOut, GENERATION + 1);
        Track preceding = track(GENERATION + 1, new Position(STOP_PAGE + 1, 0.8, "preceding text", NEW_LAYOUT));
        Assert.assertSame(preceding, this.stops.nextTrack(preceding));
        Assert.assertNull(this.stops.nextTrack(track(GENERATION + 1, relaidOut)));
    }

    @Test
    public void ignoresRatioFromOtherLayout() {
        armExact(this.stops);
        Track relaidOut = track(GENERATION, new Position(STOP_PAGE + 1, 0.8, "preceding text", NEW_LAYOUT));
        Assert.assertSame(relaidOut, this.stops.nextTrack(relaidOut));
        Assert.assertNull(
                this.stops.nextTrack(track(GENERATION, new Position(STOP_PAGE + 2, 0.9, "page12", NEW_LAYOUT))));
    }

    @Test
    public void checksFirstTrackBeyondEmptyPages() {
        armPages(this.stops);
        Assert.assertNull(this.stops.firstTrack(track(GENERATION, position(STOP_PAGE + 1, 1))));
        Assert.assertSame(TRACK, this.stops.firstTrack(TRACK));
    }

    @Test
    public void checksFirstTrackAtSameUtteranceBoundary() {
        armExact(this.stops);
        Assert.assertNull(this.stops.firstTrack(TRACK));
        Assert.assertSame(TRACK, this.stops.firstTrack(TRACK));
    }

    @Test
    public void keepsManualAdvanceOnItsOwnThread() throws InterruptedException {
        armExact(this.stops);
        this.stops.beginManualAdvance();
        Track inside = track(GENERATION, FIRST);
        java.util.concurrent.atomic.AtomicReference<Object> result = new java.util.concurrent.atomic.AtomicReference<>();
        Thread completion = new Thread(() -> result.set(this.stops.nextTrack(inside)));
        completion.start();
        completion.join();
        Assert.assertSame(inside, result.get());
        Assert.assertSame(TRACK, this.stops.nextTrack(TRACK));
        this.stops.endManualAdvance();
    }

    private static void armExact(ChapterStops stops) {
        Assert.assertSame(THIRD, stops.chapterEnd(Arrays.asList(FIRST, THIRD)));
        Assert.assertEquals(StopNotice.CHAPTER, stops.setChapterEnd(THIRD, THIRD));
    }

    private static void armPages(ChapterStops stops) {
        stops.chapterEnd(Arrays.asList(THIRD));
        stops.setChapterEnd(null, null);
    }

    private static Position position(int page, double ratio) {
        return new Position(page, ratio, "page" + page);
    }

    private static Track track(int generation, Object position) {
        return new Track(generation, position);
    }

    private static final class Position implements ReadingPosition, ContentsEntry {

        private final int page;

        private final double ratio;

        private final String key;

        private final int level;

        private final String link;

        private final int layout;

        Position(int page, double ratio, String key) {
            this(page, ratio, key, LAYOUT);
        }

        Position(int page, double ratio, String key, int layout) {
            this(page, ratio, key, 0, null, layout);
        }

        Position(int page, double ratio, String key, int level, String link) {
            this(page, ratio, key, level, link, LAYOUT);
        }

        Position(int page, double ratio, String key, int level, String link, int layout) {
            this.page = page;
            this.ratio = ratio;
            this.key = key;
            this.level = level;
            this.link = link;
            this.layout = layout;
        }

        @Override
        public int readingPage() {
            return this.page;
        }

        @Override
        public int readingPagesCount() {
            return STOP_PAGE + 2;
        }

        @Override
        public int readingType() {
            return 20;
        }

        @Override
        public int readingLayoutHash() {
            return this.layout;
        }

        @Override
        public double readingRatio() {
            return this.ratio;
        }

        @Override
        public int contentsLevel() {
            return this.level;
        }

        @Override
        public String contentsTitle() {
            return this.key;
        }

        @Override
        public String contentsLink() {
            return this.link;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Position && this.key.equals(((Position) other).key);
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

    }

    private static final class Track implements SpeechTrack {

        private final int generation;

        private final Object position;

        Track(int generation, Object position) {
            this.generation = generation;
            this.position = position;
        }

        @Override
        public int speechGeneration() {
            return this.generation;
        }

        @Override
        public Object spokenPosition() {
            return this.position;
        }

    }

}

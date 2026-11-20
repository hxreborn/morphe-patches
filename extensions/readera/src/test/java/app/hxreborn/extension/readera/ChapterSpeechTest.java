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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public final class ChapterSpeechTest {

    private static final int TEXT = 20;

    private static final int FOOTNOTE = 22;

    private static final int LAST_PAGE = 1;

    private static final Position CONTENTS_ENTRY = new Position(0, TEXT);

    @Test
    public void resolvesFootnoteBoundaryToFollowingTextOnSamePage() {
        Position marker = new Position(0, FOOTNOTE);
        Position following = new Position(0, TEXT);
        Resolver resolver = new Resolver(marker, Arrays.asList(new Position(0, TEXT), marker, following));
        Assert.assertSame(following, ChapterSpeech.playableStart(resolver, CONTENTS_ENTRY));
    }

    @Test
    public void resolvesFootnoteBoundaryOnFollowingPage() {
        Position marker = new Position(0, FOOTNOTE);
        Position following = new Position(LAST_PAGE, TEXT);
        Resolver resolver = new Resolver(marker, Arrays.asList(marker, following));
        Assert.assertSame(following, ChapterSpeech.playableStart(resolver, CONTENTS_ENTRY));
    }

    @Test
    public void skipsConsecutiveFootnoteMarkers() {
        Position marker = new Position(0, FOOTNOTE);
        Position following = new Position(LAST_PAGE, TEXT);
        Resolver resolver = new Resolver(marker,
                Arrays.asList(marker, new Position(0, FOOTNOTE), new Position(LAST_PAGE, FOOTNOTE), following));
        Assert.assertSame(following, ChapterSpeech.playableStart(resolver, CONTENTS_ENTRY));
    }

    @Test
    public void keepsFootnoteBoundaryWithoutFollowingText() {
        Position marker = new Position(LAST_PAGE, FOOTNOTE);
        Resolver resolver = new Resolver(marker, Collections.singletonList(marker));
        Assert.assertSame(marker, ChapterSpeech.playableStart(resolver, CONTENTS_ENTRY));
    }

    @Test
    public void keepsFootnoteBoundaryMissingFromItsPage() {
        Position marker = new Position(0, FOOTNOTE);
        Resolver resolver = new Resolver(marker, Collections.singletonList(new Position(0, TEXT)));
        Assert.assertSame(marker, ChapterSpeech.playableStart(resolver, CONTENTS_ENTRY));
    }

    @Test
    public void keepsPlayableBoundary() {
        Position boundary = new Position(0, TEXT);
        Resolver resolver = new Resolver(boundary, Collections.emptyList());
        Assert.assertSame(boundary, ChapterSpeech.playableStart(resolver, CONTENTS_ENTRY));
    }

    @Test
    public void passesMissingBoundary() {
        Assert.assertNull(ChapterSpeech.playableStart(new Resolver(null, Collections.emptyList()), CONTENTS_ENTRY));
    }

    private static final class Resolver implements SpeechBoundaryResolver {

        private final Object start;

        private final List<Position> utterances;

        Resolver(Object start, List<Position> utterances) {
            this.start = start;
            this.utterances = utterances;
        }

        @Override
        public List<?> contents() {
            return Collections.emptyList();
        }

        @Override
        public Object speechStart(Object position) {
            return this.start;
        }

        @Override
        public List<?> pageSpeech(int page) {
            List<Position> onPage = new ArrayList<>();
            for (Position utterance : this.utterances) {
                if (utterance.readingPage() == page) {
                    onPage.add(utterance);
                }
            }
            return onPage;
        }

    }

    private static final class Position implements ReadingPosition {

        private final int page;

        private final int type;

        Position(int page, int type) {
            this.page = page;
            this.type = type;
        }

        @Override
        public int readingPage() {
            return this.page;
        }

        @Override
        public int readingPagesCount() {
            return LAST_PAGE + 1;
        }

        @Override
        public int readingType() {
            return this.type;
        }

        @Override
        public int readingLayoutHash() {
            return 0;
        }

        @Override
        public double readingRatio() {
            return (double) this.page / LAST_PAGE;
        }

    }

}

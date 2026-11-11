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

import java.util.List;

final class ChapterStops {

    private static final int PAGES_WITHOUT_CONTENTS = 10;

    private static final int NO_GENERATION = 0;

    private final ThreadLocal<Boolean> manualAdvance = new ThreadLocal<>();

    private Object pendingStart;

    private Object pendingEntry;

    private Object resolvingStart;

    private Object resolvingEntry;

    private String chapterTitle;

    private boolean chapterFound;

    private int resolvingGeneration = NO_GENERATION;

    private StopPoint stop;

    synchronized void request(Object start, Object entry) {
        this.pendingStart = start;
        this.pendingEntry = entry;
    }

    synchronized boolean onSpeechStart(Object start, boolean restart, int generation) {
        if (start != null && start == this.pendingStart) {
            this.resolvingStart = start;
            this.resolvingEntry = this.pendingEntry;
            this.chapterTitle = (this.resolvingEntry instanceof ContentsEntry)
                    ? ((ContentsEntry) this.resolvingEntry).contentsTitle() : "";
            this.pendingStart = null;
            this.pendingEntry = null;
            this.chapterFound = false;
            this.resolvingGeneration = generation;
            this.stop = null;
            return true;
        }
        this.resolvingStart = null;
        this.resolvingEntry = null;
        this.stop = (restart && this.stop != null) ? this.stop.withRestartGeneration(generation) : null;
        return false;
    }

    synchronized Object restartBoundary(int generation) {
        return (this.stop != null && this.stop.lastGeneration() == generation) ? this.stop.boundary() : null;
    }

    synchronized void setRestartUtterance(Object boundary, Object utterance, int generation) {
        if (utterance != null && this.stop != null && this.stop.lastGeneration() == generation
                && this.stop.boundary() == boundary) {
            this.stop = this.stop.withUtterance(utterance);
        }
    }

    synchronized Object chapterEnd(List<?> contents) {
        if (!(this.resolvingEntry instanceof ContentsEntry) || !(this.resolvingEntry instanceof ReadingPosition)) {
            return null;
        }
        ContentsEntry pressed = (ContentsEntry) this.resolvingEntry;
        int page = ((ReadingPosition) this.resolvingEntry).readingPage();
        for (Object entry : contents) {
            if (!this.chapterFound) {
                this.chapterFound = entry == this.resolvingEntry;
                continue;
            }
            if (!(entry instanceof ContentsEntry) || !(entry instanceof ReadingPosition)) {
                continue;
            }
            ContentsEntry candidate = (ContentsEntry) entry;
            if (candidate.contentsLink() == null && candidate.contentsLevel() <= pressed.contentsLevel()
                    && ((ReadingPosition) entry).readingPage() >= page) {
                return entry;
            }
        }
        return null;
    }

    synchronized StopNotice setChapterEnd(Object end, Object utterance) {
        int startPage = ((ReadingPosition) this.resolvingStart).readingPage();
        StopNotice notice;
        if (this.resolvingEntry == null || !this.chapterFound) {
            this.stop = new StopPoint(this.resolvingGeneration, null, null, startPage + PAGES_WITHOUT_CONTENTS);
            notice = (this.resolvingEntry == null) ? StopNotice.TEN_PAGES : StopNotice.MISSING_CHAPTER;
        } else if (end == null) {
            this.stop = null;
            notice = StopNotice.BOOK_END;
        } else {
            int endPage = Math.max(((ReadingPosition) end).readingPage(), startPage + 1);
            this.stop = new StopPoint(this.resolvingGeneration, end, utterance, endPage);
            notice = StopNotice.CHAPTER;
        }
        this.resolvingStart = null;
        this.resolvingEntry = null;
        return notice;
    }

    synchronized String chapterTitle() {
        return this.chapterTitle;
    }

    void beginManualAdvance() {
        this.manualAdvance.set(Boolean.TRUE);
    }

    void endManualAdvance() {
        this.manualAdvance.remove();
    }

    synchronized Object lookAhead(Object position, int generation) {
        boolean reached = this.stop != null && this.stop.coversGeneration(generation)
                && this.stop.isReachedBy(position);
        return reached ? null : position;
    }

    synchronized Object firstTrack(Object track) {
        return filterTrack(track, false);
    }

    synchronized Object nextTrack(Object track) {
        return filterTrack(track, Boolean.TRUE.equals(this.manualAdvance.get()));
    }

    private Object filterTrack(Object track, boolean manual) {
        if (this.stop == null || !(track instanceof SpeechTrack)) {
            return track;
        }
        SpeechTrack next = (SpeechTrack) track;
        if (next.speechGeneration() < this.stop.firstGeneration()) {
            return track;
        }
        if (next.speechGeneration() > this.stop.lastGeneration()) {
            this.stop = null;
            return track;
        }
        if (!this.stop.isReachedBy(next.spokenPosition())) {
            return track;
        }
        this.stop = null;
        return manual ? track : null;
    }

}

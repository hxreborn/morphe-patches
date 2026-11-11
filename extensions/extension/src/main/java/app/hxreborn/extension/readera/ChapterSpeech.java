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
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class ChapterSpeech {

    static final String TITLE = "Read chapter aloud";
    static final String TEN_PAGES = "Read 10 pages aloud";

    private static final String CHAPTER_NOTICE = "Stops at the end of \"%s\"";

    private static final String BOOK_END_NOTICE = "Reads to the end of the book";

    private static final String TEN_PAGES_NOTICE = "Stops after 10 pages";

    private static final String MISSING_CHAPTER_NOTICE = "Chapter end not found. Stops after 10 pages";

    private static final int FOOTNOTE_MARKER = 22;

    private static final ChapterStops STOPS = new ChapterStops();

    private static Context application;

    private ChapterSpeech() {
    }

    public static void attachContentsRow(View row, ContentsRow contentsRow) {
        row.setOnLongClickListener(new ContentsRowMenu(row, contentsRow));
    }

    public static void attachContentsPlaceholder(TextView placeholder, ContentsPanel panel) {
        new ContentsPlaceholderAction(placeholder, panel).attach();
    }

    static void request(Context context, Object start, Object entry) {
        application = context.getApplicationContext();
        STOPS.request(start, entry);
    }

    public static Object startPosition(SpeechBoundaryResolver resolver, Object start, boolean restart, int generation,
            Object visible) {
        if (STOPS.onSpeechStart(start, restart, generation)) {
            Object end = STOPS.chapterEnd(resolver.contents());
            showNotice(STOPS.setChapterEnd(end, (end != null) ? playableStart(resolver, end) : null));
            return start;
        }
        Object boundary = STOPS.restartBoundary(generation);
        if (boundary != null) {
            STOPS.setRestartUtterance(boundary, playableStart(resolver, boundary), generation);
        }
        return visible;
    }

    static Object playableStart(SpeechBoundaryResolver resolver, Object position) {
        Object utterance = resolver.speechStart(position);
        if (!isFootnoteMarker(utterance)) {
            return utterance;
        }
        int page = ((ReadingPosition) utterance).readingPage();
        int pagesCount = ((ReadingPosition) utterance).readingPagesCount();
        List<?> pageUtterances = resolver.pageSpeech(page);
        int markerIndex = pageUtterances.indexOf(utterance);
        if (markerIndex < 0) {
            return utterance;
        }
        List<?> following = pageUtterances.subList(markerIndex + 1, pageUtterances.size());
        while (true) {
            for (Object candidate : following) {
                if (candidate instanceof ReadingPosition && !isFootnoteMarker(candidate)) {
                    return candidate;
                }
            }
            page++;
            if (page >= pagesCount) {
                return utterance;
            }
            following = resolver.pageSpeech(page);
        }
    }

    private static boolean isFootnoteMarker(Object utterance) {
        return utterance instanceof ReadingPosition && ((ReadingPosition) utterance).readingType() == FOOTNOTE_MARKER;
    }

    private static void showNotice(StopNotice notice) {
        String message;
        switch (notice) {
            case CHAPTER:
                message = String.format(Locale.ROOT, CHAPTER_NOTICE, STOPS.chapterTitle());
                break;
            case BOOK_END:
                message = BOOK_END_NOTICE;
                break;
            case TEN_PAGES:
                message = TEN_PAGES_NOTICE;
                break;
            case MISSING_CHAPTER:
                message = MISSING_CHAPTER_NOTICE;
                break;
            default:
                throw new IllegalStateException("Unknown chapter stop notice: " + notice);
        }
        Context context = application;
        if (context != null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
    }

    public static Object lookAhead(Object position, int generation) {
        return STOPS.lookAhead(position, generation);
    }

    public static boolean playsFirstTrack(Object track) {
        return STOPS.firstTrack(track) != null;
    }

    public static Object nextTrack(Object track) {
        return STOPS.nextTrack(track);
    }

    public static void beginManualAdvance() {
        STOPS.beginManualAdvance();
    }

    public static void endManualAdvance() {
        STOPS.endManualAdvance();
    }

}

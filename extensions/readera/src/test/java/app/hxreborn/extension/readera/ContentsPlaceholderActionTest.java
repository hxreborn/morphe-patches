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

import android.content.Context;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class ContentsPlaceholderActionTest {

    private static final String MESSAGE = "Table of Contents is not available for this document";

    @Test
    public void appendsTenPagesAction() {
        TextView placeholder = placeholder();
        new ContentsPlaceholderAction(placeholder, null).attach();
        Assert.assertEquals(MESSAGE + "\n\n" + ChapterSpeech.TEN_PAGES, placeholder.getText().toString());
        Assert.assertTrue(placeholder.hasOnClickListeners());
    }

    @Test
    public void clickReadsAloudFromCurrentPosition() {
        Position position = new Position();
        Reader reader = new Reader(position);
        Panel panel = new Panel(reader);
        TextView placeholder = placeholder();
        new ContentsPlaceholderAction(placeholder, panel).attach();
        placeholder.performClick();
        Assert.assertSame(position, reader.readFrom);
        Assert.assertTrue(panel.closed);
    }

    @Test
    public void clickWithoutReadingPositionKeepsContentsOpen() {
        Reader reader = new Reader(new Object());
        Panel panel = new Panel(reader);
        TextView placeholder = placeholder();
        new ContentsPlaceholderAction(placeholder, panel).attach();
        placeholder.performClick();
        Assert.assertNull(reader.readFrom);
        Assert.assertFalse(panel.closed);
    }

    @Test
    public void clickWithoutReaderKeepsContentsOpen() {
        Panel panel = new Panel(null);
        TextView placeholder = placeholder();
        new ContentsPlaceholderAction(placeholder, panel).attach();
        placeholder.performClick();
        Assert.assertFalse(panel.closed);
    }

    private static TextView placeholder() {
        Context context = RuntimeEnvironment.getApplication();
        TextView placeholder = new TextView(context);
        placeholder.setText(MESSAGE);
        return placeholder;
    }

    private static final class Panel implements ContentsPanel {

        private final ChapterReader reader;

        private boolean closed;

        Panel(ChapterReader reader) {
            this.reader = reader;
        }

        @Override
        public ChapterReader contentsReader() {
            return this.reader;
        }

        @Override
        public void closeContents() {
            this.closed = true;
        }

    }

    private static final class Reader implements ChapterReader {

        private final Object position;

        private Object readFrom;

        Reader(Object position) {
            this.position = position;
        }

        @Override
        public Object currentReadingPosition() {
            return this.position;
        }

        @Override
        public Object copyReadingPosition(Object position) {
            return position;
        }

        @Override
        public void readAloudFrom(Object position) {
            this.readFrom = position;
        }

    }

    private static final class Position implements ReadingPosition {

        @Override
        public int readingPage() {
            return 0;
        }

        @Override
        public int readingPagesCount() {
            return 1;
        }

        @Override
        public int readingType() {
            return 0;
        }

        @Override
        public int readingLayoutHash() {
            return 0;
        }

        @Override
        public double readingRatio() {
            return 0;
        }

    }

}

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

import android.content.Intent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class NotesActivityTest {

    private static final int REVIEW_TYPE = 1;

    private static final int BOOKMARK_TYPE = 2;

    private static final int QUOTE_TYPE = 3;

    private static final String BOOK_TITLE = "Book title";

    @Test
    public void excludeBookmarksFromSharing() {
        Assert.assertFalse(NotesActivity.canShare(BOOKMARK_TYPE));
    }

    @Test
    public void shareQuotesAndReviews() {
        Assert.assertTrue(NotesActivity.canShare(QUOTE_TYPE));
        Assert.assertTrue(NotesActivity.canShare(REVIEW_TYPE));
    }

    @Test
    public void shareQuoteAndAttachedNote() {
        Assert.assertEquals("Quote\n\nNote", NotesActivity.shareText("Quote", "Note"));
    }

    @Test
    public void shareNoteWithoutQuote() {
        Assert.assertEquals("Note", NotesActivity.shareText(null, "Note"));
    }

    @Test
    public void shareQuoteWithoutNote() {
        Assert.assertEquals("Quote", NotesActivity.shareText("Quote", null));
    }

    @Test
    public void shareQuoteAsPlainTextWithBookTitle() {
        Intent intent = NotesActivity.shareIntent("Quote", "Note", BOOK_TITLE);
        Assert.assertEquals(Intent.ACTION_SEND, intent.getAction());
        Assert.assertEquals("text/plain", intent.getType());
        Assert.assertEquals("Quote\n\nNote", intent.getStringExtra(Intent.EXTRA_TEXT));
        Assert.assertEquals(BOOK_TITLE, intent.getStringExtra(Intent.EXTRA_SUBJECT));
        Assert.assertNull(intent.getComponent());
    }

}

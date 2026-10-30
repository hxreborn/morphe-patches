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
package app.hxreborn.extension.imo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class LongTextMessagesTest {

    private static final int LIMIT = 2000;

    private static String repeated(String unit, int length) {
        final StringBuilder text = new StringBuilder();
        while (text.length() < length) {
            text.append(unit);
        }
        return text.substring(0, length);
    }

    private static String join(String[] parts) {
        return String.join("", parts);
    }

    @Test
    public void keepsNullAsOneMessage() {
        assertNull(LongTextMessages.splitIfOverLimit(null));
    }

    @Test
    public void keepsTextAtTheLimitAsOneMessage() {
        assertNull(LongTextMessages.splitIfOverLimit(repeated("a", LIMIT)));
    }

    @Test
    public void splitsTextOverTheLimitIntoTwoMessages() {
        // given
        final String text = repeated("a", LIMIT + 1);

        // when
        final String[] parts = LongTextMessages.splitIfOverLimit(text);

        // then
        assertEquals(2, parts.length);
        assertEquals(text, join(parts));
    }

    @Test
    public void splitsFiveThousandCharactersIntoThreeMessages() {
        // given
        final String text = repeated("abcdefghij", 5000);

        // when
        final String[] parts = LongTextMessages.splitIfOverLimit(text);

        // then
        assertEquals(3, parts.length);
        assertEquals(LIMIT, parts[0].length());
        assertEquals(LIMIT, parts[1].length());
        assertEquals(1000, parts[2].length());
        assertEquals(text, join(parts));
    }

    @Test
    public void breaksAfterWhitespaceNearTheLimit() {
        // given
        final String text = repeated("word ", 5000);

        // when
        final String[] parts = LongTextMessages.splitIfOverLimit(text);

        // then
        for (int i = 0; i < parts.length - 1; i++) {
            assertTrue(parts[i].length() <= LIMIT);
            assertTrue(parts[i].endsWith(" "));
        }
        assertEquals(text, join(parts));
    }

    @Test
    public void cutsAtTheLimitWhenNoWhitespaceIsNear() {
        // given
        final String text = repeated("a", 300) + " " + repeated("b", 3000);

        // when
        final String[] parts = LongTextMessages.splitIfOverLimit(text);

        // then
        assertEquals(LIMIT, parts[0].length());
        assertEquals(text, join(parts));
    }

    @Test
    public void keepsSurrogatePairsTogether() {
        // given
        final String text = repeated("a", LIMIT - 1) + "\uD83D\uDE00" + repeated("a", 10);

        // when
        final String[] parts = LongTextMessages.splitIfOverLimit(text);

        // then
        assertEquals(LIMIT - 1, parts[0].length());
        assertFalse(Character.isHighSurrogate(parts[0].charAt(parts[0].length() - 1)));
        assertEquals(text, join(parts));
    }

    @Test
    public void givesTheFirstPartTheOriginalImdata() throws JSONException {
        // given
        final JSONObject imdata = new JSONObject("{\"msg_id\":\"first\"}");

        // when
        final JSONObject[] perPart = LongTextMessages.imdataPerPart(imdata, 3);

        // then
        assertSame(imdata, perPart[0]);
        assertEquals(3, perPart.length);
    }

    @Test
    public void copiesImdataForLaterPartsWithoutTheReply() throws JSONException {
        // given
        final JSONObject imdata = new JSONObject("{\"msg_id\":\"first\",\"replyTo\":{\"id\":1}}");

        // when
        final JSONObject[] perPart = LongTextMessages.imdataPerPart(imdata, 2);
        perPart[0].put("msg_id", "changed");

        // then
        assertEquals("first", perPart[1].getString("msg_id"));
        assertTrue(perPart[0].has("replyTo"));
        assertFalse(perPart[1].has("replyTo"));
    }

    @Test
    public void passesMissingImdataThrough() {
        // when
        final JSONObject[] perPart = LongTextMessages.imdataPerPart(null, 2);

        // then
        assertEquals(2, perPart.length);
        assertNull(perPart[0]);
        assertNull(perPart[1]);
    }

    @Test
    public void skipsTheSendIntervalOnceAfterItIsRequested() {
        // given
        LongTextMessages.skipNextSendInterval();

        // when
        final boolean first = LongTextMessages.consumeSkippedSendInterval();
        final boolean second = LongTextMessages.consumeSkippedSendInterval();

        // then
        assertTrue(first);
        assertFalse(second);
    }

}

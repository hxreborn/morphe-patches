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
package app.hxreborn.extension.redreader;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class FilterRulesTest {

    @Test
    public void matchTitleKeywordsIgnoringCase() {
        FilterRules rules = new FilterRules("spoiler\npolitics", "", "");
        assertTrue(rules.matches("Contains a SPOILER", "pics", null));
        assertTrue(rules.matches("Politics today", "pics", null));
        assertFalse(rules.matches("A landscape", "pics", null));
    }

    @Test
    public void matchSubredditNamesExactly() {
        FilterRules rules = new FilterRules("", "news", "");
        assertTrue(rules.matches("Title", "NEWS", null));
        assertFalse(rules.matches("Title", "worldnews", null));
    }

    @Test
    public void matchSubredditRegularExpressions() {
        FilterRules rules = new FilterRules("", "/^(world)?news$/", "");
        assertTrue(rules.matches("Title", "WorldNews", null));
        assertFalse(rules.matches("Title", "newsroom", null));
    }

    @Test
    public void matchPostFlairWithoutMatchingTitle() {
        FilterRules rules = new FilterRules("", "", "spoiler");
        assertTrue(rules.matches("Title", "pics", "Possible SPOILERS"));
        assertFalse(rules.matches("spoiler", "pics", null));
    }

    @Test
    public void treatPlainKeywordsAsLiterals() {
        FilterRules rules = new FilterRules("a.b", "", "");
        assertTrue(rules.matches("a.b", null, null));
        assertFalse(rules.matches("axb", null, null));
    }

    @Test
    public void acceptBlankLinesAndWindowsLineEndings() {
        FilterRules rules = new FilterRules("  first  \r\n\r\nsecond\r\n", "", "");
        assertTrue(rules.matches("second", null, null));
        assertFalse(rules.matches("other", null, null));
    }

    @Test
    public void ignoreNullAndEmptyFields() {
        FilterRules rules = new FilterRules("keyword", "news", "flair");
        assertFalse(rules.matches(null, null, null));
        assertFalse(rules.matches("", "", ""));
    }

    @Test
    public void rejectInvalidAndEmptyExpressions() {
        assertThrows(IllegalArgumentException.class, () -> new FilterRules("", "/[/", ""));
        assertThrows(IllegalArgumentException.class, () -> new FilterRules("//", "", ""));
    }

    @Test
    public void matchUnicodeCaseVariants() {
        assertTrue(new FilterRules("café", "", "").matches("CAFÉ", null, null));
    }

}

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

import org.junit.Test;

import org.junit.Assert;

public class SpeechTextFilterTest {

    @Test
    public void omitDefaultBrackets() {
        Assert.assertEquals(" English  Spanish  English  Spanish  English  Spanish ",
                new SpeechTextFilter(SpeechTextFilter.DEFAULT_CONFIGURATION)
                    .filter("【English】〖Spanish〗〈English〉《Spanish》「English」『Spanish』"));
    }

    @Test
    public void omitSymbolsLiterally() {
        Assert.assertEquals("a  b   c  d", new SpeechTextFilter("*\n[+]\n.").filter("a* b[+]c. d"));
    }

    @Test
    public void omitWholeWordsRegardlessOfCase() {
        Assert.assertEquals("cathedral     ! cats", new SpeechTextFilter("cat").filter("cathedral CAT ! cats"));
    }

    @Test
    public void preserveUnicodeWordBoundaries() {
        Assert.assertEquals("     caféine café2 café_ café́",
                new SpeechTextFilter("café").filter("CAFÉ caféine café2 café_ café́"));
    }

    @Test
    public void preserveOffsetsForSupplementarySymbols() {
        String source = "Before 🐈 after";
        String result = new SpeechTextFilter("🐈").filter(source);
        Assert.assertEquals("Before    after", result);
        Assert.assertEquals(source.length(), result.length());
        Assert.assertEquals(source.indexOf("after"), result.indexOf("after"));
    }

    @Test
    public void omitEveryOverlappingEntryFromOriginalText() {
        Assert.assertEquals("     ", new SpeechTextFilter("a b\nb c").filter("a b c"));
    }

    @Test
    public void omitOverlappingOccurrencesOfSameEntry() {
        Assert.assertEquals("   ", new SpeechTextFilter("++").filter("+++"));
    }

    @Test
    public void preserveOriginalTextWhenConfigurationIsEmpty() {
        String source = "Book * text";
        Assert.assertSame(source, new SpeechTextFilter("\n \r\n").filter(source));
    }

    @Test
    public void preserveNullInput() {
        Assert.assertNull(new SpeechTextFilter("*").filter(null));
    }

    @Test
    public void omitEntireUtteranceWithoutChangingLength() {
        Assert.assertEquals("     ", new SpeechTextFilter("*\nword").filter("*word"));
    }

    @Test
    public void acceptWindowsLineEndingsAndTrimEntries() {
        Assert.assertEquals("    ", new SpeechTextFilter(" a \r\n * ").filter("a **"));
    }

}

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
package app.hxreborn.extension.netanalyzer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ExportTableTest {

    private static String csvRow(String... cells) {
        final ExportTable table = new ExportTable(true, ",", "A", "B");
        for (String cell : cells) {
            table.add(cell);
        }
        table.endRow();
        return table.toString().split("\r\n")[1];
    }

    @Test
    public void prefixesACellThatStartsAFormula() {
        // when
        final String row = csvRow("=HYPERLINK(\"x\")", "@SUM(A1)");

        // then
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\",'@SUM(A1)", row);
    }

    @Test
    public void keepsSignedNumbersAsNumbers() {
        // when
        final String row = csvRow("-65", "+1.5");

        // then
        assertEquals("-65,+1.5", row);
    }

    @Test
    public void prefixesASignedCellThatIsNotANumber() {
        // when
        final String row = csvRow("-2+3+cmd|' /C calc'!A0", "+x");

        // then
        assertEquals("'-2+3+cmd|' /C calc'!A0,'+x", row);
    }

}

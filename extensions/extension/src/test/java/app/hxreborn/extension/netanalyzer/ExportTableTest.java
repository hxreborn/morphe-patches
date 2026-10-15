/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

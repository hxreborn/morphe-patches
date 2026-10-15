/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ExportTable {

    private static final String CSV_LINE_END = "\r\n";

    private static final String TEXT_LINE_END = "\n";

    private static final String TEXT_FIELD_PREFIX = "- ";

    private static final String TEXT_FIELD_SEPARATOR = ": ";

    private static final String QUOTE = "\"";

    private static final String ESCAPED_QUOTE = "\"\"";

    private static final char NEWLINE = '\n';

    private static final char CARRIAGE_RETURN = '\r';

    private static final char SPACE = ' ';

    private static final String FORMULA_PREFIXES = "=+-@\t";

    private static final String FORMULA_GUARD = "'";

    private final boolean csv;

    private final String separator;

    private List<String> header;

    private final List<String> cells = new ArrayList<>();

    private final StringBuilder output = new StringBuilder();

    ExportTable(boolean csv, String separator, String... header) {
        this.csv = csv;
        this.separator = separator;
        this.header = Arrays.asList(header);
    }

    boolean isCsv() {
        return this.csv;
    }

    ExportTable add(String value) {
        this.cells.add((value != null) ? value.replace(NEWLINE, SPACE).replace(CARRIAGE_RETURN, SPACE) : "");
        return this;
    }

    ExportTable addRaw(String value) {
        this.cells.add(value);
        return this;
    }

    ExportTable addLong(long value) {
        this.cells.add(String.valueOf(value));
        return this;
    }

    ExportTable addAddress(InetAddress address) {
        final String host = (address != null) ? address.getHostAddress() : null;
        this.cells.add((host != null) ? host : "");
        return this;
    }

    void endRow() {
        if (this.csv) {
            if (this.header != null) {
                appendCsvLine(this.header);
                this.header = null;
            }
            appendCsvLine(this.cells);
        } else {
            this.output.append(this.cells.get(0)).append(TEXT_LINE_END);
            for (int column = 1; column < this.cells.size(); column++) {
                if (!this.cells.get(column).isEmpty()) {
                    this.output.append(TEXT_FIELD_PREFIX)
                        .append(this.header.get(column))
                        .append(TEXT_FIELD_SEPARATOR)
                        .append(this.cells.get(column))
                        .append(TEXT_LINE_END);
                }
            }
            this.output.append(TEXT_LINE_END);
        }
        this.cells.clear();
    }

    @Override
    public String toString() {
        return this.output.toString();
    }

    private void appendCsvLine(List<String> line) {
        for (int column = 0; column < line.size(); column++) {
            if (column > 0) {
                this.output.append(this.separator);
            }
            this.output.append(escape(line.get(column)));
        }
        this.output.append(CSV_LINE_END);
    }

    private String escape(String cell) {
        final String text = guardFormula(cell);
        if (!text.contains(QUOTE) && !text.contains(this.separator)) {
            return text;
        }
        return QUOTE + text.replace(QUOTE, ESCAPED_QUOTE) + QUOTE;
    }

    private static String guardFormula(String cell) {
        if (cell.isEmpty() || FORMULA_PREFIXES.indexOf(cell.charAt(0)) < 0 || isNumber(cell)) {
            return cell;
        }
        return FORMULA_GUARD + cell;
    }

    private static boolean isNumber(String cell) {
        try {
            Double.parseDouble(cell);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

}

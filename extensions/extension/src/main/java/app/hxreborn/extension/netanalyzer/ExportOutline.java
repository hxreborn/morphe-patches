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

import java.net.InetAddress;

final class ExportOutline {

    static final int INDENT_STEP = 4;

    private static final String LINE_END = "\n";

    private static final String BULLET = "- ";

    private static final String CONTINUATION = "  ";

    private static final String LABEL_SEPARATOR = ": ";

    private static final String NOT_AVAILABLE = "N/A";

    private static final String SPACE = " ";

    private final StringBuilder output = new StringBuilder();

    private int indent;

    void indent(int columns) {
        this.indent += columns;
    }

    void title(String title) {
        if (this.indent == 0) {
            if (this.output.length() > 0) {
                this.output.append(LINE_END);
            }
            this.output.append(title).append(LINE_END);
        } else {
            appendIndent();
            this.output.append(BULLET).append(title).append(LINE_END);
        }
    }

    void line(String label, String value) {
        line(label, value, true);
    }

    void line(String label, InetAddress address) {
        line(label, (address != null) ? address.getHostAddress() : null);
    }

    void line(String label, String value, boolean bullet) {
        appendIndent();
        this.output.append(bullet ? BULLET : CONTINUATION)
            .append(label)
            .append(LABEL_SEPARATOR)
            .append((value == null || value.isEmpty()) ? NOT_AVAILABLE : value)
            .append(LINE_END);
    }

    private void appendIndent() {
        for (int column = 0; column < this.indent; column++) {
            this.output.append(SPACE);
        }
    }

    @Override
    public String toString() {
        return this.output.toString();
    }

}

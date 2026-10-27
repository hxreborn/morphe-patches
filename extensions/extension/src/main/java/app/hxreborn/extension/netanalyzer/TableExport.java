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

import android.content.Context;

abstract class TableExport implements ExportSource {

    abstract String[] header();

    abstract void writeRows(Context context, ExportTable table);

    @Override
    public String text(Context context) {
        return table(context, false, ExportConfiguration.DEFAULT_SEPARATOR);
    }

    String csv(Context context) {
        return table(context, true, ExportConfiguration.separator(context));
    }

    private String table(Context context, boolean csv, String separator) {
        final ExportTable table = new ExportTable(csv, separator, header());
        writeRows(context, table);
        return table.toString();
    }

}

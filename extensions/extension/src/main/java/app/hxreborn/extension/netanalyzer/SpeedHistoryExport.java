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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;

final class SpeedHistoryExport extends TableExport {

    private static final String[] HEADER = { "Name", "Type", "Date", "Download Speed", "Upload Speed" };

    private static final String DATE_PATTERN = "M/d/yy, H:mm";

    @Override
    public String subject(Context context) {
        return "Speed History";
    }

    @Override
    String[] header() {
        return HEADER;
    }

    @Override
    void writeRows(Context context, ExportTable table) {
        final SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        final List<SpeedHistory.Entry> entries = SpeedHistory.load(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            final SpeedHistory.Entry entry = entries.get(i);
            table.add(entry.name)
                .add(entry.connectionType)
                .add((entry.date != 0) ? format.format(new Date(entry.date)) : "")
                .add(SpeedFormat.both(entry.downloadBytesPerSecond))
                .add(SpeedFormat.both(entry.uploadBytesPerSecond))
                .endRow();
        }
    }

}

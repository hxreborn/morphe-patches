/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

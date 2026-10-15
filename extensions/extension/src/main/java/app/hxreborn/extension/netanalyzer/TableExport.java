/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

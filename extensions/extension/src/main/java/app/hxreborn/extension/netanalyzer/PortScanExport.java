/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused")
public final class PortScanExport extends TableExport {

    private static final String TAG = "hxNetAnalyzer";

    private static final String[] HEADER = { "Port Start", "Port End", "Scanned Port Number", "Port Status", "Protocol",
            "Service Name", "Service Description" };

    private static final String PROTOCOL = "TCP";

    private static final String OPEN = "OPEN";

    private static final String CLOSED = "CLOSED";

    private static final String RANGE_SEPARATOR = "-";

    private static final int START = 0;

    private static final int END = 1;

    private static final int STATE = 2;

    private static Class<?> portScanTool;

    private static String host = "";

    public static void recordStart(Class<?> tool, String hostname) {
        portScanTool = tool;
        host = hostname;
    }

    @Override
    public String subject(Context context) {
        return "Ports '" + host + "'";
    }

    @Override
    String[] header() {
        return HEADER;
    }

    @Override
    void writeRows(Context context, ExportTable table) {
        final ServiceNames names = ServiceNames.open(context);
        try {
            for (PortRange range : ranges()) {
                if (range.open) {
                    final String application = names.application(range.start);
                    table.addLong(range.start)
                        .add("")
                        .add("")
                        .add(range.status)
                        .add(PROTOCOL)
                        .add(application)
                        .add((application != null) ? names.describe(application) : null);
                } else {
                    table.addLong(range.start)
                        .add((range.start == range.end) ? "" : String.valueOf(range.end))
                        .add("")
                        .add(range.status)
                        .add(PROTOCOL)
                        .add("")
                        .add("");
                }
                table.endRow();
            }
        } finally {
            names.close();
        }
    }

    @Override
    public String text(Context context) {
        final ExportOutline outline = new ExportOutline();
        final ServiceNames names = ServiceNames.open(context);
        try {
            for (PortRange range : ranges()) {
                outline.title((range.open || range.start == range.end) ? String.valueOf(range.start)
                        : range.start + RANGE_SEPARATOR + range.end);
                outline.line("State", range.status);
                if (range.open) {
                    final String application = names.application(range.start);
                    if (application != null) {
                        outline.line("Service Name", application);
                        final String description = names.describe(application);
                        if (!description.isEmpty()) {
                            outline.line("Service Description", description);
                        }
                    }
                }
            }
        } finally {
            names.close();
        }
        return outline.toString();
    }

    private static List<PortRange> ranges() {
        final List<PortRange> ranges = new ArrayList<>();
        if (portScanTool == null) {
            return ranges;
        }
        try {
            for (Object value : StaticStates.values(portScanTool)) {
                if (!(value instanceof List) || ((List<?>) value).isEmpty()) {
                    continue;
                }
                final Object first = ((List<?>) value).get(0);
                final Field[] fields;
                try {
                    fields = FieldLayout.instance(first.getClass(), int.class, int.class, Enum.class);
                } catch (IllegalStateException ex) {
                    continue;
                }
                for (Object range : (List<?>) value) {
                    ranges.add(new PortRange((Integer) FieldLayout.get(fields[START], range),
                            (Integer) FieldLayout.get(fields[END], range),
                            (Enum<?>) FieldLayout.get(fields[STATE], range)));
                }
                return ranges;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot read the port scan results", ex);
        }
        return ranges;
    }

    private static final class PortRange {

        final int start;

        final int end;

        final boolean open;

        final String status;

        PortRange(int start, int end, Enum<?> state) {
            this.start = start;
            this.end = end;
            this.open = OPEN.equals(state.name());
            this.status = status(state.name());
        }

        private static String status(String state) {
            if (OPEN.equals(state)) {
                return "open";
            }
            return CLOSED.equals(state) ? "closed" : "blocked";
        }

    }

}

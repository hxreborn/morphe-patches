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

final class ProbeExport extends TableExport {

    private static final String[] HEADER = { "Probe Number", "RTT [ms]", "Probe Send Time", "IP Address", "Hostname",
            "TTL", "AS Number", "AS Name", "Country Name", "Country Code", "Time Zone", "Region", "City", "Latitude",
            "Longitude" };

    private static final int LOCATION_COLUMNS = 9;

    private static final String TIME_PATTERN = "HH:mm:ss";

    private static final String DELAY_FORMAT = "%.1f";

    private static final String MILLIS_FORMAT = "%.1f ms";

    private static final String COORDINATE_FORMAT = "%.2f";

    private static final String NUMBER_PREFIX = "#";

    private static final String LOST_PERCENT_FORMAT = "%.0f %%";

    private static final double PERCENT = 100;

    private final boolean traceroute;

    private ProbeExport(boolean traceroute) {
        this.traceroute = traceroute;
    }

    static ProbeExport ping() {
        return new ProbeExport(false);
    }

    static ProbeExport traceroute() {
        return new ProbeExport(true);
    }

    @Override
    public String subject(Context context) {
        return this.traceroute ? "Route '" + HostTools.tracerouteHost() + "'" : "Ping '" + HostTools.pingHost() + "'";
    }

    @Override
    String[] header() {
        return HEADER;
    }

    @Override
    void writeRows(Context context, ExportTable table) {
        for (Probe probe : snapshot().probes) {
            final long number = probe.sequenceNumber + 1;
            table.addRaw(table.isCsv() ? String.valueOf(number) : NUMBER_PREFIX + number)
                .add(probe.pingable ? String.format(Locale.US, DELAY_FORMAT, probe.delay) : "")
                .add(time(probe))
                .addAddress(probe.address)
                .add(probe.hostname)
                .add((probe.ttl != 0) ? String.valueOf(probe.ttl) : "");
            final ExternalIp location = probe.location;
            if (location == null) {
                for (int column = 0; column < LOCATION_COLUMNS; column++) {
                    table.add("");
                }
            } else {
                table.add(location.asNumber)
                    .add(location.asName)
                    .add(location.countryName)
                    .add(location.countryCode)
                    .add(location.timeZone)
                    .add(location.region)
                    .add(location.city)
                    .add(coordinate(location, location.latitude))
                    .add(coordinate(location, location.longitude));
            }
            table.endRow();
        }
    }

    @Override
    public String text(Context context) {
        if (this.traceroute) {
            return super.text(context);
        }
        final HostTools.Snapshot snapshot = snapshot();
        final ExportOutline outline = new ExportOutline();
        hostDetails(outline, snapshot.probes);
        statistics(outline, snapshot);
        outline.title("Ping");
        for (Probe probe : snapshot.probes) {
            outline.line(NUMBER_PREFIX + (probe.sequenceNumber + 1) + " (" + time(probe) + ")",
                    probe.pingable ? String.format(Locale.US, MILLIS_FORMAT, probe.delay) : null);
        }
        return outline.toString();
    }

    private HostTools.Snapshot snapshot() {
        return this.traceroute ? HostTools.traceroute() : HostTools.ping();
    }

    private static void hostDetails(ExportOutline outline, List<Probe> probes) {
        Probe replied = null;
        for (Probe probe : probes) {
            if (probe.pingable) {
                replied = probe;
                break;
            }
        }
        if (replied == null) {
            return;
        }
        outline.title("Host Details");
        outline.line("IP Address", replied.address);
        if (replied.hostname != null) {
            outline.line("Hostname", replied.hostname);
        }
        if (replied.ttl != 0) {
            outline.line("TTL", String.valueOf(replied.ttl));
        }
        final ExternalIp location = replied.location;
        if (location == null) {
            return;
        }
        lineIfPresent(outline, "AS Number", location.asNumber);
        lineIfPresent(outline, "AS Name", location.asName);
        lineIfPresent(outline, "Country Name", location.countryName);
        lineIfPresent(outline, "Country Code", location.countryCode);
        lineIfPresent(outline, "Time Zone", location.timeZone);
        lineIfPresent(outline, "Region", location.region);
        lineIfPresent(outline, "City", location.city);
        if (location.city != null) {
            outline.line("Latitude", coordinate(location, location.latitude));
            outline.line("Longitude", coordinate(location, location.longitude));
        }
    }

    private static void statistics(ExportOutline outline, HostTools.Snapshot snapshot) {
        final PingStatistics statistics = PingStatistics.of(snapshot.probes, snapshot.active);
        if (statistics.received == 0) {
            return;
        }
        outline.title("Statistics");
        outline.line("Minimum", String.format(Locale.US, MILLIS_FORMAT, statistics.min));
        outline.line("Maximum", String.format(Locale.US, MILLIS_FORMAT, statistics.max));
        outline.line("Median", String.format(Locale.US, MILLIS_FORMAT, statistics.median));
        outline.line("Lost", statistics.lost + " pkt ("
                + String.format(Locale.US, LOST_PERCENT_FORMAT, PERCENT * statistics.lostFraction) + ")");
    }

    private static void lineIfPresent(ExportOutline outline, String label, String value) {
        if (value != null) {
            outline.line(label, value);
        }
    }

    private static String coordinate(ExternalIp location, Double value) {
        return (location.city != null && value != null) ? String.format(Locale.US, COORDINATE_FORMAT, value) : "";
    }

    private static String time(Probe probe) {
        return new SimpleDateFormat(TIME_PATTERN, Locale.US).format((probe.time != null) ? probe.time : new Date());
    }

}

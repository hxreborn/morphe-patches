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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;

final class BonjourExport extends TableExport {

    private static final String[] HEADER = { "Name", "Type", "Description", "Hostname", "IPv4 Address", "IPv6 Address",
            "Port", "Values" };

    private static final String VALUE_SEPARATOR = ", ";

    private static final String KEY_VALUE_SEPARATOR = " = ";

    private final List<BonjourService> services;

    BonjourExport(List<BonjourService> services) {
        this.services = services;
    }

    @Override
    public String subject(Context context) {
        return "Bonjour Services of '" + ConnectedNetwork.ssid(context) + "'";
    }

    @Override
    String[] header() {
        return HEADER;
    }

    @Override
    void writeRows(Context context, ExportTable table) {
        final Map<String, String> applications = new TreeMap<>();
        for (BonjourService service : this.services) {
            if (service.type != null) {
                applications.put(service.type, (service.application != null) ? service.application : "");
            }
        }
        final ServiceNames names = ServiceNames.open(context);
        try {
            for (Map.Entry<String, String> type : applications.entrySet()) {
                final String description = names.describe(type.getValue());
                for (BonjourService service : this.services) {
                    if (type.getKey().equals(service.type)) {
                        table.add(service.name)
                            .add(service.type)
                            .add(description)
                            .add(service.server)
                            .addAddress(service.address4)
                            .addAddress(service.address6)
                            .addLong(service.port)
                            .add(values(service.txt))
                            .endRow();
                    }
                }
            }
        } finally {
            names.close();
        }
    }

    private static String values(Map<String, String> txt) {
        final List<String> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : txt.entrySet()) {
            entries.add(entry.getKey() + KEY_VALUE_SEPARATOR + entry.getValue());
        }
        return String.join(VALUE_SEPARATOR, entries);
    }

}

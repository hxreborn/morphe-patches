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

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Probe {

    private static final String FIELD_SEPARATOR = ",";

    private static final String NAME_SEPARATOR = "=";

    final InetAddress address;

    final String hostname;

    final ExternalIp location;

    final List<Double> delays;

    final double delay;

    final boolean pingable;

    final int sequenceNumber;

    final boolean destination;

    final int ttl;

    final boolean inFlight;

    final Date time;

    private Probe(Object probe, Map<String, Field> fields) throws ReflectiveOperationException {
        this.address = (InetAddress) fields.get("address").get(probe);
        this.hostname = (String) fields.get("hostname").get(probe);
        final Object locationValue = fields.get("location").get(probe);
        this.location = (locationValue != null) ? ExternalIp.of(this.address, this.hostname, locationValue) : null;
        final Object delayValues = fields.get("delays").get(probe);
        final List<Double> values = new ArrayList<>();
        if (delayValues instanceof List) {
            for (Object value : (List<?>) delayValues) {
                values.add(((Number) value).doubleValue());
            }
        }
        this.delays = Collections.unmodifiableList(values);
        this.delay = fields.get("delay").getDouble(probe);
        this.pingable = fields.get("pingable").getBoolean(probe);
        this.sequenceNumber = fields.get("seqNumber").getInt(probe);
        this.destination = fields.get("isDestination").getBoolean(probe);
        this.ttl = fields.get("ttl").getInt(probe);
        this.inFlight = fields.get("inFlight").getBoolean(probe);
        this.time = (Date) fields.get("time").get(probe);
    }

    static Probe read(Object probe, String fieldNames) throws ReflectiveOperationException {
        return new Probe(probe, fields(probe.getClass(), fieldNames));
    }

    static List<Probe> readAll(Iterable<?> probes, String fieldNames) throws ReflectiveOperationException {
        final List<Probe> result = new ArrayList<>();
        Map<String, Field> fields = null;
        for (Object probe : probes) {
            if (fields == null) {
                fields = fields(probe.getClass(), fieldNames);
            }
            result.add(new Probe(probe, fields));
        }
        return result;
    }

    private static Map<String, Field> fields(Class<?> type, String fieldNames) throws NoSuchFieldException {
        final Map<String, Field> fields = new HashMap<>();
        for (String entry : fieldNames.split(FIELD_SEPARATOR)) {
            final String[] parts = entry.split(NAME_SEPARATOR, 2);
            final Field field = type.getDeclaredField(parts[1]);
            field.setAccessible(true);
            fields.put(parts[0], field);
        }
        return fields;
    }

}

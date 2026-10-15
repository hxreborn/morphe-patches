/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

final class ToolOptions {

    static final String PING_PROTOCOL = "pingProtocol";

    static final String PING_DELAY = "pingDelay";

    static final String PING_TCP_PORT = "pingTcpPort";

    static final String PAYLOAD_SIZE = "payloadSize";

    static final String DONT_FRAGMENT = "dontFragment";

    static final String AUDIBLE = "audible";

    static final String MAX_DELAY = "maxDelay";

    static final String MIN_DELAY = "minDelay";

    static final String TRACEROUTE_UDP = "tracerouteUdp";

    static final String PORTSCAN_DELAY = "portscanDelay";

    static final String WHOIS_SERVER = "whoisServer";

    static final String RDAP_SERVER = "rdapServer";

    static final String DNSSEC = "dnssec";

    static final String NO_RECURSION = "norec";

    static final String SHOW_ALL_SECTIONS = "showAllSections";

    private static final String PREFERENCES = "hxToolOptions";

    private static final String FIELD_SEPARATOR = ",";

    private static final String NAME_SEPARATOR = "=";

    private ToolOptions() {
    }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static Object apply(Context context, Object item, String fieldNames) throws ReflectiveOperationException {
        final SharedPreferences preferences = preferences(context);
        for (Map.Entry<String, String> property : parse(fieldNames).entrySet()) {
            if (!preferences.contains(property.getKey())) {
                continue;
            }
            final Field field = item.getClass().getDeclaredField(property.getValue());
            field.setAccessible(true);
            final Class<?> type = field.getType();
            if (type == int.class) {
                field.setInt(item, preferences.getInt(property.getKey(), field.getInt(item)));
            } else if (type == boolean.class) {
                field.setBoolean(item, preferences.getBoolean(property.getKey(), field.getBoolean(item)));
            } else if (type == double.class) {
                field.setDouble(item, preferences.getFloat(property.getKey(), (float) field.getDouble(item)));
            } else if (type == String.class) {
                final String value = preferences.getString(property.getKey(), null);
                field.set(item, (value == null || value.trim().isEmpty()) ? null : value.trim());
            }
        }
        return item;
    }

    private static Map<String, String> parse(String fieldNames) {
        final Map<String, String> fields = new HashMap<>();
        for (String entry : fieldNames.split(FIELD_SEPARATOR)) {
            final String[] parts = entry.split(NAME_SEPARATOR, 2);
            fields.put(parts[0], parts[1]);
        }
        return fields;
    }

}

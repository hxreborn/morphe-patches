/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused")
public final class DnsExport extends TableExport {

    private static final String TAG = "hxNetAnalyzer";

    private static final String[] HEADER = { "Type", "Section", "Name", "TTL", "Value" };

    private static final String[] SECTIONS = { "ANSWER", "AUTHORITY", "ADDITIONAL" };

    private static final int ANSWER = 0;

    private static final String REGISTRATION_SEPARATOR = ";";

    private static final String LIST_SEPARATOR = ",";

    private static final String NAME_SEPARATOR = "=";

    private static final int SECTION_FIELDS = 0;

    private static final int RECORD_FIELDS = 1;

    private static final int TYPE_SOA = 6;

    private static final List<Integer> PRIORITY_TYPES = Arrays.asList(15, 33, 64, 65);

    private static final Comparator<DnsRecord> RECORD_ORDER = (left, right) -> {
        if ((left.typeCode == TYPE_SOA) != (right.typeCode == TYPE_SOA)) {
            return (left.typeCode == TYPE_SOA) ? -1 : 1;
        }
        final int byType = left.type.compareTo(right.type);
        if (byType != 0 || !PRIORITY_TYPES.contains(left.typeCode)) {
            return byType;
        }
        return Integer.compare(left.priority, right.priority);
    };

    private static Class<?> mnemonicHolder;

    private static String[] sectionFields;

    private static final Map<String, String> RECORD_FIELD_NAMES = new HashMap<>();

    private static Object response;

    private static boolean showAllSections;

    private static String host = "";

    public static void register(Class<?> typeMnemonics, String registration) {
        final String[] parts = registration.split(REGISTRATION_SEPARATOR);
        mnemonicHolder = typeMnemonics;
        sectionFields = parts[SECTION_FIELDS].split(LIST_SEPARATOR);
        RECORD_FIELD_NAMES.clear();
        for (String entry : parts[RECORD_FIELDS].split(LIST_SEPARATOR)) {
            final String[] names = entry.split(NAME_SEPARATOR, 2);
            RECORD_FIELD_NAMES.put(names[0], names[1]);
        }
    }

    public static void observe(Object dnsResponse, boolean allSections, String hostname) {
        response = dnsResponse;
        showAllSections = allSections;
        host = (hostname != null) ? hostname : "";
    }

    @Override
    public String subject(Context context) {
        return "DNS '" + host + "'";
    }

    @Override
    String[] header() {
        return HEADER;
    }

    @Override
    void writeRows(Context context, ExportTable table) {
        final Object current = response;
        if (current == null || sectionFields == null) {
            return;
        }
        try {
            final Method mnemonic = mnemonicMethod();
            final Object mnemonics = mnemonicTable();
            final int sections = showAllSections ? sectionFields.length : ANSWER + 1;
            for (int section = 0; section < sections; section++) {
                final Field field = current.getClass().getDeclaredField(sectionFields[section]);
                field.setAccessible(true);
                final Object[] records = (Object[]) field.get(current);
                if (records == null) {
                    continue;
                }
                final List<DnsRecord> sorted = new ArrayList<>();
                for (Object record : records) {
                    sorted.add(new DnsRecord(record, (String) mnemonic.invoke(mnemonics, intField(record, "type"))));
                }
                Collections.sort(sorted, RECORD_ORDER);
                for (DnsRecord record : sorted) {
                    table.add(record.type)
                        .add(SECTIONS[section])
                        .add(record.name)
                        .addLong(record.ttl)
                        .add(record.value)
                        .endRow();
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot read the DNS response", ex);
        }
    }

    private static Object mnemonicTable() throws ReflectiveOperationException {
        for (Field field : mnemonicHolder.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                field.setAccessible(true);
                return field.get(null);
            }
        }
        throw new NoSuchFieldException("No mnemonic table in " + mnemonicHolder);
    }

    private static Method mnemonicMethod() throws ReflectiveOperationException {
        final Object table = mnemonicTable();
        for (Class<?> type = table.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() == String.class
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[] { int.class })
                        && !Modifier.isStatic(method.getModifiers())) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new NoSuchMethodException("No type mnemonic lookup on " + table.getClass());
    }

    private static Object field(Object record, String property) throws ReflectiveOperationException {
        final Field field = record.getClass().getDeclaredField(RECORD_FIELD_NAMES.get(property));
        field.setAccessible(true);
        return field.get(record);
    }

    private static int intField(Object record, String property) throws ReflectiveOperationException {
        return ((Number) field(record, property)).intValue();
    }

    private static final class DnsRecord {

        final int typeCode;

        final String type;

        final String name;

        final long ttl;

        final String value;

        final int priority;

        DnsRecord(Object record, String type) throws ReflectiveOperationException {
            this.typeCode = intField(record, "type");
            this.type = type;
            this.name = (String) field(record, "name");
            this.ttl = ((Number) field(record, "ttl")).longValue();
            this.value = (String) field(record, "rdata");
            this.priority = intField(record, "priority");
        }

    }

}
